package org.openyaske.app

import androidx.compose.animation.togetherWith

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.openyaske.app.resources.Res
import org.openyaske.app.ui.CalendarScreen
import org.openyaske.app.ui.CourseDetailSheet
import org.openyaske.app.ui.Destination
import org.openyaske.app.ui.GpaScreen
import org.openyaske.app.ui.HomeScreen
import org.openyaske.app.ui.LicensesScreen
import org.openyaske.app.ui.LocalUiStrings
import org.openyaske.app.ui.PdfScreen
import org.openyaske.app.ui.MoreScreen
import org.openyaske.app.ui.OpenYaskeTheme
import org.openyaske.app.ui.initialWebPath
import org.openyaske.app.ui.initialWebQuery
import org.openyaske.app.ui.observeWebBack
import org.openyaske.app.ui.pushWebPath
import org.openyaske.app.ui.replaceWebPath
import org.openyaske.app.ui.SearchScreen
import org.openyaske.app.ui.SettingsScreen
import org.openyaske.app.ui.TasksScreen
import org.openyaske.app.ui.TimetableScreen
import org.openyaske.app.ui.design.AppNavigationBar
import org.openyaske.app.ui.design.SideRail
import org.openyaske.app.ui.uiStrings
import org.openyaske.core.api.CoreApi
import org.openyaske.core.validation.SyllabusBundleCodec

@OptIn(ExperimentalResourceApi::class, ExperimentalComposeUiApi::class)
@Composable
fun App(api: CoreApi) {
    val settings by api.settings.collectAsState()
    val strings = remember(settings.locale) { uiStrings(settings.locale) }

    // 起動直後はシラバスの読み込みを待つ間、今日のカードと同じ形のスケルトンを出す。
    var syllabusReady by remember { mutableStateOf(api.syllabusSize > 0) }
    LaunchedEffect(Unit) {
        if (api.syllabusSize == 0) {
            runCatching {
                val bytes = Res.readBytes("files/syllabus.cbor")
                api.setSyllabus(SyllabusBundleCodec.decode(bytes).toEntries())
            }
        }
        syllabusReady = true
    }

    val fontFamily = org.openyaske.app.ui.scriptFontFamily(settings.locale)
    CompositionLocalProvider(LocalUiStrings provides strings) {
        OpenYaskeTheme(settings.theme, fontFamily, settings.accentHue, settings.accentStyle == org.openyaske.core.api.AccentStyle.MONOCHROME) {
            if (!syllabusReady) {
                BootSkeleton()
                return@OpenYaskeTheme
            }
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(org.openyaske.app.ui.design.LocalAppColors.current.background)
            ) {
            // 高さ 480dp 未満は横画面コンパクトとして、ヘッダー・下部余白・列数を詰める。
            val compactLayout = maxHeight < 480.dp
            CompositionLocalProvider(
                org.openyaske.app.ui.design.LocalCompactLayout provides compactLayout
            ) {
            val startPath = remember { initialWebPath() }
            val startQuery = remember { initialWebQuery() }
            LaunchedEffect(Unit) {
                // 見た目の確認用。?hue=140&mono=1 でアクセントを差し替える。
                startQuery?.let { query ->
                    val hue = query.substringAfter("hue=", "").substringBefore('&').toIntOrNull()
                    val mono = when {
                        query.contains("mono=1") -> true
                        query.contains("mono=0") -> false
                        else -> null
                    }
                    if (hue != null || mono != null) {
                        runCatching {
                            api.updateSettings { current ->
                                current.copy(
                                    accentHue = hue ?: current.accentHue,
                                    accentStyle = mono?.let {
                                        if (it) org.openyaske.core.api.AccentStyle.MONOCHROME
                                        else org.openyaske.core.api.AccentStyle.COLOR
                                    } ?: current.accentStyle
                                )
                            }
                        }
                    }
                }
                // 見た目の確認用。?theme=dark / ?theme=light で切り替える。
                when {
                    startQuery?.contains("theme=black") == true ->
                        api.updateSettings { it.copy(theme = org.openyaske.core.api.ThemeMode.PURE_BLACK) }
                    startQuery?.contains("theme=dark") == true ->
                        api.updateSettings { it.copy(theme = org.openyaske.core.api.ThemeMode.DARK) }
                    startQuery?.contains("theme=light") == true ->
                        api.updateSettings { it.copy(theme = org.openyaske.core.api.ThemeMode.LIGHT) }
                }
            }
            var destination by remember { mutableStateOf(destinationFromPath(startPath)) }
            var showSettings by remember { mutableStateOf(startPath?.startsWith("/settings") == true) }
            var settingsScrollTo by remember { mutableStateOf<Int?>(null) }
            var showGpa by remember { mutableStateOf(startPath?.startsWith("/gpa") == true) }
            var showSearch by remember { mutableStateOf(startPath?.startsWith("/search") == true) }
            var showLicenses by remember { mutableStateOf(startPath?.startsWith("/licenses") == true) }
            var showPdf by remember { mutableStateOf(startPath?.startsWith("/pdf") == true) }
            var selectedCourse by remember { mutableStateOf(courseIdFromPath(startPath)) }
            var lastPath by remember { mutableStateOf<String?>(null) }
            var backNavigation by remember { mutableStateOf(false) }

            val path = when {
                selectedCourse != null -> "/course/$selectedCourse"
                showSearch -> "/search"
                showLicenses -> "/licenses"
                showPdf -> "/pdf"
                showSettings -> "/settings"
                showGpa -> "/gpa"
                else -> pathOf(destination)
            }
            // 履歴は「タブ切替 = 1 段」だけ積む。詳細へ入るのは置き換え、シートは積まない。
            LaunchedEffect(path) {
                when {
                    lastPath == null -> replaceWebPath(path)
                    backNavigation -> {
                        replaceWebPath(path)
                        backNavigation = false
                    }
                    isTabPath(path) && lastPath?.let { isTabPath(it) } == true -> pushWebPath(path)
                    else -> replaceWebPath(path)
                }
                lastPath = path
            }

            fun restoreFromPath(target: String?) {
                selectedCourse = courseIdFromPath(target)
                showSearch = target?.startsWith("/search") == true
                showLicenses = target?.startsWith("/licenses") == true
                showPdf = target?.startsWith("/pdf") == true
                showSettings = target?.startsWith("/settings") == true
                showGpa = target?.startsWith("/gpa") == true
                destination = destinationFromPath(target)
                lastPath = target
            }

            // ブラウザの戻るで URL が変わったら、URL から画面を組み直す。
            LaunchedEffect(Unit) {
                observeWebBack { restoreFromPath(initialWebPath()) }
            }

            // 戻るは 3 段。シート/ダイアログ → 詳細 → （タブのルートでは消費しない）。
            val canGoBack = selectedCourse != null || showSettings || showGpa || showSearch ||
                showLicenses || showPdf
            fun goBack() {
                backNavigation = true
                when {
                    selectedCourse != null -> selectedCourse = null
                    showSearch -> showSearch = false
                    showLicenses -> showLicenses = false
                    showPdf -> showPdf = false
                    showSettings -> showSettings = false
                    showGpa -> showGpa = false
                }
            }
            // ui-backhandler 1.11 の推奨 API（NavigationEventHandler）はまだ公開されていない
            @Suppress("DEPRECATION")
            BackHandler(enabled = canGoBack) { goBack() }

            // 画面の切替はフェードで繋ぐ。詳細（下位画面）は URL を置き換えるだけで履歴は増やさない。
            val screenKey = when {
                showSearch -> "search"
                showLicenses -> "licenses"
                showPdf -> "pdf"
                showSettings -> "settings"
                showGpa -> "gpa"
                else -> "main"
            }
            androidx.compose.animation.AnimatedContent(
                targetState = screenKey,
                transitionSpec = {
                    androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(180)
                    ).togetherWith(
                        androidx.compose.animation.fadeOut(
                            animationSpec = androidx.compose.animation.core.tween(120)
                        )
                    )
                },
                label = "screen",
                modifier = Modifier.fillMaxSize()
            ) { screen ->
                when (screen) {
                    "search" -> SearchScreen(
                        api = api,
                        onCourse = { selectedCourse = it },
                        bottomInset = false,
                        onBack = { goBack() }
                    )
                    "licenses" -> LicensesScreen { goBack() }
                    "pdf" -> PdfScreen { goBack() }
                    "settings" -> SettingsScreen(
                        api = api,
                        onClose = { goBack() },
                        startAtItem = settingsScrollTo
                    )
                    "gpa" -> GpaScreen(api) { goBack() }
                    else -> MainScaffold(
                        api = api,
                        destination = destination,
                        onDestination = { destination = it },
                        onSettings = {
                            settingsScrollTo = null
                            showSettings = true
                        },
                        onCalendarSettings = {
                            settingsScrollTo = CALENDAR_SETTINGS_INDEX
                            showSettings = true
                        },
                        onGpa = { showGpa = true },
                        onSearch = { showSearch = true },
                        onLicenses = { showLicenses = true },
                        onPdf = { showPdf = true },
                        onCourse = { selectedCourse = it }
                    )
                }
            }

            selectedCourse?.let { courseId ->
                CourseDetailSheet(api, courseId) { goBack() }
            }
            }
            }
        }
    }
}

/** 起動中に出す 1 枚。今日のカードと同じ 28dp 角丸で、読み込み中だと分かる形にする。 */
@Composable
private fun BootSkeleton() {
    val colors = org.openyaske.app.ui.design.LocalAppColors.current
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = org.openyaske.app.ui.design.Layout.gutter, vertical = 16.dp)
    ) {
        androidx.compose.foundation.layout.Column {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(org.openyaske.app.ui.design.AppShapes.sheet)
                    .background(colors.primaryContainer)
            )
            Spacer(Modifier.height(16.dp))
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(org.openyaske.app.ui.design.AppShapes.card)
                    .background(colors.surface)
            )
        }
    }
}

/** 設定の「カレンダー」節（表示・成績・時間割の各 2 項目の後）。 */
private const val CALENDAR_SETTINGS_INDEX = 6

private fun isTabPath(path: String): Boolean =
    path == "/" || path == "/todo" || path == "/timetable" || path == "/calendar" || path == "/more"

private fun destinationFromPath(path: String?): Destination = when {
    path == null -> Destination.HOME
    path.startsWith("/todo") -> Destination.TASKS
    path.startsWith("/timetable") -> Destination.TIMETABLE
    path.startsWith("/calendar") -> Destination.CALENDAR
    path.startsWith("/more") -> Destination.MORE

    else -> Destination.HOME
}

private fun courseIdFromPath(path: String?): String? {
    if (path == null || !path.startsWith("/course/")) return null
    return path.removePrefix("/course/").substringBefore('?').ifBlank { null }
}

private fun pathOf(destination: Destination): String = when (destination) {
    Destination.HOME -> "/"
    Destination.TASKS -> "/todo"
    Destination.TIMETABLE -> "/timetable"
    Destination.CALENDAR -> "/calendar"
    Destination.MORE -> "/more"
}

@Composable
private fun MainScaffold(
    api: CoreApi,
    destination: Destination,
    onDestination: (Destination) -> Unit,
    onSettings: () -> Unit,
    onCalendarSettings: () -> Unit,
    onGpa: () -> Unit,
    onSearch: () -> Unit,
    onLicenses: () -> Unit,
    onPdf: () -> Unit,
    onCourse: (String) -> Unit
) {
    val colors = org.openyaske.app.ui.design.LocalAppColors.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        Row(modifier = Modifier.fillMaxSize()) {
            if (wide) {
                SideRail(destination = destination, onDestination = onDestination)
            }
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                ScreenContent(
                    api = api,
                    destination = destination,
                    onDestination = onDestination,
                    onSettings = onSettings,
                    onCalendarSettings = onCalendarSettings,
                    onGpa = onGpa,
                    onSearch = onSearch,
                    onLicenses = onLicenses,
                    onPdf = onPdf,
                    onCourse = onCourse,
                    bottomInset = !wide
                )
                if (!wide) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(colors.background)
                    ) {
                        AppNavigationBar(
                            destination = destination,
                            onDestination = onDestination
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenContent(
    api: CoreApi,
    destination: Destination,
    onDestination: (Destination) -> Unit,
    onSettings: () -> Unit,
    onCalendarSettings: () -> Unit,
    onGpa: () -> Unit,
    onSearch: () -> Unit,
    onLicenses: () -> Unit,
    onPdf: () -> Unit,
    onCourse: (String) -> Unit,
    bottomInset: Boolean
) {
    androidx.compose.animation.AnimatedContent(
        targetState = destination,
        transitionSpec = {
            androidx.compose.animation.fadeIn(
                animationSpec = androidx.compose.animation.core.tween(180)
            ).togetherWith(
                androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(120)
                )
            )
        },
        label = "destination",
        modifier = Modifier.fillMaxSize()
    ) { current ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (current) {
                Destination.HOME -> HomeScreen(
                    api = api,
                    onNavigate = onDestination,
                    onCourse = onCourse,
                    onSettings = onSettings,
                    onSearch = onSearch,
                    bottomInset = bottomInset
                )
                Destination.TASKS -> TasksScreen(api, onCourse, onSettings, bottomInset)
                Destination.TIMETABLE -> TimetableScreen(api, onCourse, onSettings, onGpa, bottomInset)
                Destination.CALENDAR -> CalendarScreen(api, onCourse, onCalendarSettings, bottomInset)
                Destination.MORE -> MoreScreen(
                    api = api,
                    onSearch = onSearch,
                    onGpa = onGpa,
                    onSettings = onSettings,
                    onLicenses = onLicenses,
                    onPdf = onPdf,
                    bottomInset = bottomInset
                )
            }
        }
    }
}

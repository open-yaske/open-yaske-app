package org.openyaske.app.ui

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.openyaske.app.resources.Res
import org.openyaske.app.resources.app_icon
import org.openyaske.app.ui.initialWebQuery
import org.openyaske.app.ui.design.AppCard
import org.openyaske.app.ui.design.AppIcons
import org.openyaske.app.ui.design.AppShapes
import org.openyaske.app.ui.design.CircleIconButton
import org.openyaske.app.ui.design.Disclosure
import org.openyaske.app.ui.design.HairLine
import org.openyaske.app.ui.design.Layout
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.Radius
import org.openyaske.app.ui.design.SectionBar
import org.openyaske.app.ui.design.Space
import org.openyaske.app.ui.design.parseCourseColor
import org.openyaske.app.ui.design.screenBottomPadding
import org.openyaske.app.ui.design.tabular
import org.openyaske.app.ui.design.tintOf
import org.openyaske.core.api.AppSettings
import org.openyaske.core.api.CoreApi
import org.openyaske.core.logic.PeriodTimes
import org.openyaske.core.model.Course
import org.openyaske.core.model.CourseSession
import org.openyaske.core.model.SessionStatus
import org.openyaske.core.validation.IsoDate

/** ヒーローが示す 6 状態。 */
private enum class HeroState {
    BEFORE_CLASS,
    IN_CLASS,
    DONE_TODAY,
    HOLIDAY,
    OUT_OF_TERM,
    EMPTY_TIMETABLE
}

/**
 * ホーム。
 *
 * 上から、あいさつと日付、次の授業までのカウントダウン、よく使うサービス、今日の時間割。
 * 天気と運行は取得元が無いので、カードごと出さない（空の箱を置かない）。
 */
@Composable
fun HomeScreen(
    api: CoreApi,
    onNavigate: (Destination) -> Unit,
    onCourse: (String) -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
    bottomInset: Boolean = true
) {
    val data by api.state.collectAsState()
    val settings by api.settings.collectAsState()
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    val today = remember { todayIso() }

    // 1 分ごとに数え直す。
    var minuteTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            minuteTick += 1
        }
    }
    val now = remember(minuteTick) { nowMinutes() }

    val coursesById = remember(data) { data.courses.filter { it.deletedAtMillis == null }.associateBy { it.id } }
    val colorIndex = remember(data) {
        data.courses.sortedBy { it.name }.withIndex().associate { it.value.id to it.index }
    }
    val sessions = remember(data) {
        data.sessions
            .filter { it.deletedAtMillis == null && it.status != SessionStatus.CANCELLED }
            .sortedWith(compareBy({ it.date }, { startMinutes(it, settings) ?: 0 }))
    }
    val hero = remember(sessions, coursesById, settings, now, today) {
        resolveHero(sessions, coursesById, settings, today, now)
    }
    val todaySessions = remember(sessions, today) { sessions.filter { it.date == today } }
    val agenda = remember(data, today) { api.agenda(today, today) }
    var detailTask by remember { mutableStateOf<org.openyaske.core.model.CourseTask?>(null) }
    val otherToday = remember(agenda) { agenda.filter { it.item !is org.openyaske.core.logic.AgendaItem.Class } }
    val upcoming = remember(data, today) {
        data.courseTasks
            .filter { it.deletedAtMillis == null && it.status != org.openyaske.core.model.TaskStatus.DONE }
            .filter { task -> task.dueDate?.let { it > today } ?: true }
            .sortedBy { it.dueDate ?: "9999-12-31" }
            .take(4)
    }
    val totalCredits = remember(data) {
        data.courses.filter { it.deletedAtMillis == null }.sumOf { it.credits }
    }
    val weekCount = remember(data, today) {
        val monday = IsoDate.plusDays(today, -(IsoDate.dayOfWeekIso(today) - 1))
        val sunday = IsoDate.plusDays(monday, 6)
        data.sessions.count { it.deletedAtMillis == null && it.date in monday..sunday }
    }

    // 状態の確認用。?preview=before / inclass / done / holiday / outofterm / empty
    val preview = remember { initialWebQuery()?.substringAfter("preview=", "")?.substringBefore('&')?.ifBlank { null } }
    val forced = preview?.let { name ->
        when (name) {
            "before" -> HeroState.BEFORE_CLASS
            "inclass" -> HeroState.IN_CLASS
            "done" -> HeroState.DONE_TODAY
            "holiday" -> HeroState.HOLIDAY
            "outofterm" -> HeroState.OUT_OF_TERM
            "empty" -> HeroState.EMPTY_TIMETABLE
            else -> null
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= 900.dp
        val accent = colors.primary

        Box(
            modifier = Modifier
                .fillMaxSize()
                // 地に放射グラデーションを 1 枚だけ敷く（もやは使わない）。
                .drawWithCache {
                    val brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.85f, 0f),
                        radius = size.width
                    )
                    onDrawBehind { drawRect(brush) }
                }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxHeight().widthIn(max = 880.dp).align(Alignment.TopCenter),
                contentPadding = PaddingValues(
                    start = Layout.gutter,
                    end = Layout.gutter,
                    top = Space.sm,
                    bottom = screenBottomPadding(bottomInset)
                ),
                verticalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                item {
                    GreetingHeader(
                        today = today,
                        onSearch = onSearch,
                        onSettings = onSettings
                    )
                }

                item {
                    // 状態の確認用に preview を付けたときは、データが無くても代表値を出す。
                    val displaySession = if (forced != null) hero.session ?: sessions.firstOrNull() else hero.session
                    val displayNext = if (forced != null) hero.nextSession ?: sessions.lastOrNull() else hero.nextSession
                    Hero(
                        state = forced ?: hero.state,
                        minutes = if (forced != null) hero.minutes.takeIf { it > 0 } ?: 23 else hero.minutes,
                        session = displaySession,
                        course = displaySession?.let { coursesById[it.courseId] },
                        nextSession = displayNext,
                        daysLater = if (forced != null) hero.daysLater.takeIf { it > 0 } ?: 2 else hero.daysLater,
                        settings = settings,
                        locale = settings.locale,
                        onTimetable = { onNavigate(Destination.TIMETABLE) },
                        onSettings = onSettings
                    )
                }

                item {
                    WeatherTransitCards(
                        api = api,
                        settings = settings,
                        wide = wide
                    )
                }

                item {
                    StatRow(
                        todayCount = todaySessions.size,
                        weekCount = weekCount,
                        credits = totalCredits
                    )
                }

                item { HomeLinks() }

                item {
                    TodayList(
                        sessions = todaySessions,
                        coursesById = coursesById,
                        colorIndex = colorIndex,
                        settings = settings,
                        now = now,
                        onCourse = onCourse
                    )
                }

                if (otherToday.isNotEmpty()) {
                    item { TodayOtherItems(otherToday, onTask = { detailTask = it }, onNavigate = onNavigate) }
                }

                item {
                    UpcomingSection(
                        tasks = upcoming,
                        coursesById = coursesById,
                        colorIndex = colorIndex,
                        today = today,
                        onCourse = onCourse,
                        onTask = { detailTask = it },
                        onNavigate = onNavigate
                    )
                }

            }
        }
    }

    detailTask?.let { task ->
        TaskDetailSheet(
            api = api,
            task = task,
            course = task.courseId?.let { coursesById[it] },
            onEdit = { detailTask = null; onNavigate(Destination.TASKS) },
            onDismiss = { detailTask = null }
        )
    }
}

/* ---------------- あいさつ ---------------- */

@Composable
private fun GreetingHeader(today: String, onSearch: () -> Unit, onSettings: () -> Unit) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    val hour = remember { nowMinutes() / 60 }
    val greeting = when {
        hour < 5 -> t["home_greeting_evening"]
        hour < 11 -> t["home_greeting_morning"]
        hour < 18 -> t["home_greeting_day"]
        else -> t["home_greeting_evening"]
    }
    val date = remember(today, t["lang"]) { longDateLabel(today, t["lang"]) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // タブ画面の TabHeader と同じく、ステータスバーの下から始める。
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Text(
                text = date,
                style = MaterialTheme.typography.titleMedium,
                color = colors.text
            )
        }
        CircleIconButton(
            icon = AppIcons.search,
            contentDescription = t["home_search"],
            onClick = onSearch,
            size = 40.dp
        )
        Spacer(Modifier.width(Space.xs))
        CircleIconButton(
            icon = AppIcons.settings,
            contentDescription = t["settings"],
            onClick = onSettings,
            size = 40.dp
        )
    }
}

/* ---------------- ヒーロー ---------------- */

private data class HeroContent(
    val state: HeroState,
    val minutes: Int,
    val session: CourseSession?,
    val nextSession: CourseSession?,
    val daysLater: Int
)

private fun startMinutes(session: CourseSession, settings: AppSettings): Int? =
    session.startTime?.let { PeriodTimes.parseTimeToMinutes(it) }
        ?: session.startPeriod?.let { settings.periodStartMinutes[it] ?: PeriodTimes.uecStartMinutes[it] }

private fun endMinutes(session: CourseSession, settings: AppSettings): Int? {
    session.endTime?.let { return PeriodTimes.parseTimeToMinutes(it) }
    val start = startMinutes(session, settings) ?: return null
    val lastPeriod = (session.startPeriod ?: 1) + session.periodCount - 1
    return PeriodTimes.uecEndMinutes[lastPeriod] ?: (start + 90)
}

private fun resolveHero(
    sessions: List<CourseSession>,
    courses: Map<String, Course>,
    settings: AppSettings,
    today: String,
    now: Int
): HeroContent {
    if (sessions.isEmpty()) {
        return if (courses.isEmpty()) {
            HeroContent(HeroState.EMPTY_TIMETABLE, 0, null, null, 0)
        } else {
            HeroContent(HeroState.OUT_OF_TERM, 0, null, null, 0)
        }
    }
    val withCourse = sessions.filter { courses.containsKey(it.courseId) }
    if (withCourse.isEmpty()) {
        return HeroContent(
            if (courses.isEmpty()) HeroState.EMPTY_TIMETABLE else HeroState.OUT_OF_TERM,
            0, null, null, 0
        )
    }
    val todaySessions = withCourse.filter { it.date == today }
    val current = todaySessions.firstOrNull { session ->
        val start = startMinutes(session, settings) ?: return@firstOrNull false
        val end = endMinutes(session, settings) ?: return@firstOrNull false
        now >= start && now < end
    }
    if (current != null) {
        val end = endMinutes(current, settings) ?: now
        return HeroContent(HeroState.IN_CLASS, (end - now), current, null, 0)
    }
    val nextToday = todaySessions.firstOrNull { session ->
        (startMinutes(session, settings) ?: return@firstOrNull false) > now
    }
    if (nextToday != null) {
        val start = startMinutes(nextToday, settings) ?: now
        return HeroContent(HeroState.BEFORE_CLASS, start - now, nextToday, null, 0)
    }
    val future = withCourse.filter { it.date > today }.sortedWith(
        compareBy({ it.date }, { startMinutes(it, settings) ?: 0 })
    )
    val next = future.firstOrNull()
    return when {
        todaySessions.isNotEmpty() -> HeroContent(HeroState.DONE_TODAY, 0, null, next, daysBetween(today, next?.date))
        next != null -> HeroContent(HeroState.HOLIDAY, 0, null, next, daysBetween(today, next.date))
        else -> HeroContent(HeroState.OUT_OF_TERM, 0, null, null, 0)
    }
}

private fun daysBetween(from: String, to: String?): Int {
    if (to == null) return 0
    return (IsoDate.toEpochDay(to) - IsoDate.toEpochDay(from)).toInt()
}

@Composable
private fun Hero(
    state: HeroState,
    minutes: Int,
    session: CourseSession?,
    course: Course?,
    nextSession: CourseSession?,
    daysLater: Int,
    settings: AppSettings,
    locale: String,
    onTimetable: () -> Unit,
    onSettings: () -> Unit
) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    val strong = state == HeroState.IN_CLASS
    val background by animateColorAsState(
        targetValue = if (strong) colors.primary else colors.primaryContainer,
        label = "heroBackground"
    )
    val onSurface = if (strong) colors.onPrimary else colors.onPrimaryContainer
    val soft = if (strong) colors.onPrimary.copy(alpha = 0.8f) else colors.textSecondary
    val today = remember { todayIso() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.hero)
            .background(background)
            .padding(horizontal = Space.xl, vertical = Space.lg)
    ) {
        when (state) {
            HeroState.BEFORE_CLASS -> {
                Text(t["home_next_class_in"], style = MaterialTheme.typography.bodySmall, color = soft)
                Text(
                    text = if (minutes <= 1) t["home_soon"] else "$minutes" + t["home_minutes_suffix"],
                    style = MaterialTheme.typography.headlineLarge.tabular(),
                    color = onSurface
                )
                session?.let {
                    Text(
                        text = heroSessionLine(it, course, settings, t),
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(onClick = onTimetable, contentPadding = PaddingValues(0.dp)) {
                    Text(t["home_open_timetable"], color = onSurface)
                }
            }

            HeroState.IN_CLASS -> {
                Text(
                    text = session?.let { heroSessionLine(it, course, settings, t) } ?: t["home_in_class"],
                    style = MaterialTheme.typography.titleMedium,
                    color = onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(t["home_in_class"], style = MaterialTheme.typography.bodySmall, color = soft)
                Text(
                    text = if (minutes <= 1) t["home_soon_end"]
                    else t["home_until_end"] + " $minutes" + t["home_minutes_suffix"],
                    style = MaterialTheme.typography.headlineMedium.tabular(),
                    color = onSurface
                )
            }

            HeroState.DONE_TODAY -> {
                Text(
                    text = t["home_done_today"],
                    style = MaterialTheme.typography.titleMedium,
                    color = onSurface
                )
                nextSession?.let {
                    Text(
                        text = nextLine(it, settings, today, locale, t),
                        style = MaterialTheme.typography.bodyMedium,
                        color = soft
                    )
                }
            }

            HeroState.HOLIDAY -> {
                nextSession?.let {
                    Text(t["home_next_class_is"], style = MaterialTheme.typography.bodySmall, color = soft)
                    Text(
                        text = nextLine(it, settings, today, locale, t),
                        style = MaterialTheme.typography.titleMedium,
                        color = onSurface
                    )
                    if (daysLater > 0) {
                        Text(
                            text = "$daysLater" + t["home_days_later"],
                            style = MaterialTheme.typography.bodySmall,
                            color = soft
                        )
                    }
                }
            }

            HeroState.OUT_OF_TERM -> {
                Text(t["home_no_next"], style = MaterialTheme.typography.titleMedium, color = onSurface)
                TextButton(onClick = onSettings, contentPadding = PaddingValues(0.dp)) {
                    Text(t["home_open_settings"], color = onSurface)
                }
            }

            HeroState.EMPTY_TIMETABLE -> {
                Text(t["home_empty_timetable"], style = MaterialTheme.typography.titleMedium, color = onSurface)
                TextButton(onClick = onTimetable, contentPadding = PaddingValues(0.dp)) {
                    Text(t["add_course"], color = onSurface)
                }
            }
        }
    }
}

private fun periodLabel(session: CourseSession, t: UiStrings): String =
    session.startPeriod?.let { "$it" + t["search_period_suffix"] } ?: ""

/** 「2限 情報基礎演習 · 西2-B101」。 */
private fun heroSessionLine(
    session: CourseSession,
    course: Course?,
    settings: AppSettings,
    t: UiStrings
): String = listOfNotNull(
    periodLabel(session, t).ifBlank { null },
    course?.name,
    session.room ?: course?.room
).joinToString(" · ")

/** 「明日 1限 9:00〜」「11/5（水）1限」。 */
private fun nextLine(
    session: CourseSession,
    settings: AppSettings,
    today: String,
    locale: String,
    t: UiStrings
): String {
    val day = when (session.date) {
        today -> t["tasks_today"]
        IsoDate.plusDays(today, 1) -> t["home_tomorrow"]
        else -> shortDateLabel(session.date, locale, t)
    }
    val start = startMinutes(session, settings)
    val time = start?.let { shortTime(it) }
    return listOfNotNull(day, periodLabel(session, t).ifBlank { null }, time)
        .joinToString(" ")
        .let { if (time == null) it else "$it〜" }
}

private fun shortTime(minutes: Int): String {
    val formatted = PeriodTimes.formatMinutes(minutes)
    return if (formatted.startsWith("0")) formatted.drop(1) else formatted
}

private fun shortDateLabel(date: String, locale: String, t: UiStrings): String {
    val month = date.substring(5, 7).toIntOrNull() ?: 0
    val day = date.substring(8).toIntOrNull() ?: 0
    val weekday = IsoDate.dayOfWeekIso(date)
    return if (locale.startsWith("ja")) {
        "${month}/${day}（${t["day_$weekday"]}）"
    } else {
        "${month}/${day} ${t["day_$weekday"]}"
    }
}

private fun longDateLabel(date: String, locale: String): String {
    val month = date.substring(5, 7).toIntOrNull() ?: 0
    val day = date.substring(8).toIntOrNull() ?: 0
    return if (locale.startsWith("ja")) "${month}月${day}日" else "${month}/${day}"
}

/* ---------------- リンク集 ---------------- */

/** 2 列グリッドのリンク。Svelte 版の Bento グリッドに合わせる。 */
@Composable
private fun HomeLinks() {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    val uriHandler = LocalUriHandler.current
    val study = remember { CampusLinkGroups.first().links.take(4) }
    val others = remember {
        CampusLinkGroups.drop(1).flatMap { it.links }.take(4)
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        SectionBar(t["home_links_campus"])
        LinkGrid(study, colors.primary, uriHandler)
        SectionBar(t["home_links_study"])
        LinkGrid(others, colors.info, uriHandler)
        SectionBar(t["home_links_all"])
        CampusLinksSection()
    }
}

@Composable
private fun LinkGrid(
    links: List<CampusLink>,
    tint: Color,
    uriHandler: androidx.compose.ui.platform.UriHandler
) {
    val colors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        links.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                row.forEach { link ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(AppShapes.card)
                            .background(colors.surface)
                            .border(1.dp, colors.border, AppShapes.card)
                            .clickable { runCatching { uriHandler.openUri(link.url) } }
                            .padding(horizontal = Space.md, vertical = Space.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(AppShapes.card)
                                .background(tintOf(tint, !colors.dark)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(link.icon, null, tint = tint, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            text = link.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.text,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(start = Space.sm)
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/* ---------------- 今日の時間割 ---------------- */

@Composable
private fun TodayList(
    sessions: List<CourseSession>,
    coursesById: Map<String, Course>,
    colorIndex: Map<String, Int>,
    settings: AppSettings,
    now: Int,
    onCourse: (String) -> Unit
) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current

    Column {
        SectionBar(t["home_today_timetable"])
        if (sessions.isEmpty()) {
            AppCard {
                Text(
                    text = t["home_no_class"],
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.md)
                )
            }
        } else {
            AppCard {
                sessions.forEachIndexed { index, session ->
                    val course = coursesById[session.courseId]
                    if (index > 0) HairLine(modifier = Modifier.padding(start = Space.lg))
                    val start = startMinutes(session, settings)
                    val end = endMinutes(session, settings)
                    val isCurrent = start != null && end != null && now >= start && now < end
                    val corners by animateDpAsState(
                        targetValue = if (isCurrent) Radius.control else Radius.xs,
                        label = "todayRowCorners"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Space.xs, vertical = 2.dp)
                            .clip(AppShapes.morph(corners))
                            .background(if (isCurrent) colors.primaryContainer.copy(alpha = 0.6f) else Color.Transparent)
                            .clickable(enabled = course != null) { course?.let { onCourse(it.id) } }
                            .height(72.dp)
                            .padding(horizontal = Space.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(32.dp)
                                .clip(AppShapes.pill)
                                .background(
                                    course?.let { parseCourseColor(it.color, colorIndex[it.id] ?: 0) }
                                        ?: colors.borderStrong
                                )
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = Space.md)) {
                            Text(
                                text = course?.name ?: "—",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = listOfNotNull(
                                    start?.let { shortTime(it) },
                                    end?.let { shortTime(it) },
                                    session.room ?: course?.room
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall.tabular(),
                                color = colors.textSecondary,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = session.startPeriod?.let { "$it" + t["search_period_suffix"] } ?: "",
                            style = MaterialTheme.typography.labelSmall.tabular(),
                            color = colors.textTertiary
                        )
                        Disclosure()
                    }
                }
            }
        }
    }
}

/* ---------------- 数字・今日の課題・今後 ---------------- */

@Composable
private fun StatRow(todayCount: Int, weekCount: Int, credits: Int) {
    val t = LocalUiStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        StatTile("$todayCount", t["home_stat_today"], Modifier.weight(1f))
        StatTile("$weekCount", t["home_stat_week"], Modifier.weight(1f))
        StatTile("$credits", t["home_stat_credits"], Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .clip(AppShapes.card)
            .background(colors.surface)
            .border(1.dp, colors.border, AppShapes.card)
            .padding(horizontal = Space.md, vertical = Space.sm),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.tabular(),
            color = colors.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TodayOtherItems(
    entries: List<org.openyaske.core.logic.AgendaEntry>,
    onTask: (org.openyaske.core.model.CourseTask) -> Unit,
    onNavigate: (Destination) -> Unit
) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    Column {
        SectionBar(t["home_today_other"])
        AppCard {
            entries.forEachIndexed { index, entry ->
                if (index > 0) HairLine(modifier = Modifier.padding(start = Space.lg))
                val (title, subtitle) = when (val item = entry.item) {
                    is org.openyaske.core.logic.AgendaItem.Task ->
                        item.task.title to (item.course?.name ?: t["task_due"])
                    is org.openyaske.core.logic.AgendaItem.Event ->
                        item.event.title to (item.event.location ?: t["event_label"])
                    else -> return@forEachIndexed
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            when (val current = entry.item) {
                                is org.openyaske.core.logic.AgendaItem.Task -> onTask(current.task)
                                else -> onNavigate(Destination.CALENDAR)
                            }
                        }
                        .padding(horizontal = Space.lg, vertical = Space.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.info)
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = Space.sm)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = colors.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Disclosure()
                }
            }
        }
    }
}

@Composable
private fun UpcomingSection(
    tasks: List<org.openyaske.core.model.CourseTask>,
    coursesById: Map<String, Course>,
    colorIndex: Map<String, Int>,
    today: String,
    onCourse: (String) -> Unit,
    onTask: (org.openyaske.core.model.CourseTask) -> Unit,
    onNavigate: (Destination) -> Unit
) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    Column {
        SectionBar(t["tasks_upcoming"])
        if (tasks.isEmpty()) {
            AppCard {
                Text(
                    text = t["home_no_task"],
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.md)
                )
            }
        } else {
            AppCard {
                tasks.forEachIndexed { index, task ->
                    if (index > 0) HairLine(modifier = Modifier.padding(start = Space.lg))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTask(task) }
                            .padding(horizontal = Space.lg, vertical = Space.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    parseCourseColor(
                                        coursesById[task.courseId]?.color,
                                        colorIndex[task.courseId] ?: 0
                                    )
                                )
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = Space.sm)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            task.dueDate?.let { due ->
                                Text(
                                    text = "${t["task_due"]} ${dueLabel(due, today, t["lang"])}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        Disclosure()
                    }
                }
            }
        }
    }
}

/* ---------------- 補助 ---------------- */

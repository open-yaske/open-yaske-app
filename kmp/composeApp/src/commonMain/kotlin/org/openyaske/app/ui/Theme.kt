package org.openyaske.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import org.openyaske.app.resources.Res
import org.openyaske.app.resources.noto_sans_jp
import org.openyaske.app.resources.noto_sans_jp_content
import org.openyaske.app.resources.noto_sans_jp_ui
import org.openyaske.app.resources.noto_sans_kr
import org.openyaske.app.resources.noto_sans_kr_content
import org.openyaske.app.resources.noto_sans_kr_ui
import org.openyaske.app.resources.noto_sans_sc
import org.openyaske.app.resources.noto_sans_sc_content
import org.openyaske.app.resources.noto_sans_sc_ui
import org.openyaske.app.ui.design.BlackAppColors
import org.openyaske.app.ui.design.DarkAppColors
import org.openyaske.app.ui.design.LightAppColors
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.withAccent
import org.openyaske.core.api.ThemeMode

private fun schemeFor(c: org.openyaske.app.ui.design.AppColors, dark: Boolean) = run {
    if (dark) {
        darkColorScheme(
            primary = c.primary,
            onPrimary = c.onPrimary,
            primaryContainer = c.primaryContainer,
            onPrimaryContainer = c.onPrimaryContainer,
            secondary = c.accent,
            onSecondary = c.onPrimary,
            secondaryContainer = c.primaryContainer,
            onSecondaryContainer = c.onPrimaryContainer,
            tertiary = c.info,
            onTertiary = c.onPrimary,
            background = c.background,
            onBackground = c.text,
            surface = c.surface,
            onSurface = c.text,
            surfaceVariant = c.sunken,
            onSurfaceVariant = c.textSecondary,
            surfaceDim = c.background,
            surfaceBright = c.surfaceAlt,
            surfaceContainerLowest = c.background,
            surfaceContainerLow = c.surfaceAlt,
            surfaceContainer = c.surface,
            surfaceContainerHigh = c.sunken,
            surfaceContainerHighest = c.border,
            inverseSurface = c.text,
            inverseOnSurface = c.background,
            outline = c.borderStrong,
            outlineVariant = c.border,
            error = c.danger,
            onError = c.onPrimary
        )
    } else {
        lightColorScheme(
            primary = c.primary,
            onPrimary = c.onPrimary,
            primaryContainer = c.primaryContainer,
            onPrimaryContainer = c.onPrimaryContainer,
            secondary = c.accent,
            onSecondary = c.onPrimary,
            secondaryContainer = c.primaryContainer,
            onSecondaryContainer = c.onPrimaryContainer,
            tertiary = c.info,
            onTertiary = c.onPrimary,
            background = c.background,
            onBackground = c.text,
            surface = c.surface,
            onSurface = c.text,
            surfaceVariant = c.sunken,
            onSurfaceVariant = c.textSecondary,
            surfaceDim = c.background,
            surfaceBright = c.surface,
            surfaceContainerLowest = c.surface,
            surfaceContainerLow = c.surfaceAlt,
            surfaceContainer = c.surface,
            surfaceContainerHigh = c.sunken,
            surfaceContainerHighest = c.border,
            inverseSurface = c.text,
            inverseOnSurface = c.background,
            outline = c.borderStrong,
            outlineVariant = c.border,
            error = c.danger,
            onError = c.onPrimary
        )
    }
}

/** MaterialTheme.shapes も [org.openyaske.app.ui.design.Radius] と同じ 4 段に合わせる。 */
private val OpenYaskeShapes = Shapes(
    extraSmall = org.openyaske.app.ui.design.AppShapes.control,
    small = org.openyaske.app.ui.design.AppShapes.control,
    medium = org.openyaske.app.ui.design.AppShapes.card,
    large = org.openyaske.app.ui.design.AppShapes.sheet,
    extraLarge = org.openyaske.app.ui.design.AppShapes.sheet
)

/** 言語ごとの本文フォント。1 ウェイトだけ登録し、太字は合成に任せる。 */
/**
 * UI 用のサブセットだけを返す。言語チップなど、その場で確実に出したい文字に使う。
 */
@Composable
fun uiFontFamily(language: String): FontFamily = when (language.take(2)) {
    "zh" -> FontFamily(Font(Res.font.noto_sans_sc_ui))
    "ko" -> FontFamily(Font(Res.font.noto_sans_kr_ui))
    else -> FontFamily(Font(Res.font.noto_sans_jp_ui))
}

/**
 * 本文フォント。先に UI 用サブセット（約 330KB）で描き、
 * フルフォントの読み込みが終わったら差し替える。
 *
 * Compose の FontFamily はグリフ単位のフォールバックをしないため、
 * 2 つ並べるのではなく、読み込み完了で家族ごと入れ替える。
 */
@Composable
fun scriptFontFamily(language: String): FontFamily {
    val ui = when (language.take(2)) {
        "zh" -> Res.font.noto_sans_sc_ui
        "ko" -> Res.font.noto_sans_kr_ui
        else -> Res.font.noto_sans_jp_ui
    }
    val content = when (language.take(2)) {
        "zh" -> Res.font.noto_sans_sc_content
        "ko" -> Res.font.noto_sans_kr_content
        else -> Res.font.noto_sans_jp_content
    }
    val full = when (language.take(2)) {
        "zh" -> Res.font.noto_sans_sc
        "ko" -> Res.font.noto_sans_kr
        else -> Res.font.noto_sans_jp
    }
    val contentPath = when (language.take(2)) {
        "zh" -> "font/noto_sans_sc_content.ttf"
        "ko" -> "font/noto_sans_kr_content.ttf"
        else -> "font/noto_sans_jp_content.ttf"
    }
    val fullPath = when (language.take(2)) {
        "zh" -> "font/noto_sans_sc.ttf"
        "ko" -> "font/noto_sans_kr.ttf"
        else -> "font/noto_sans_jp.ttf"
    }
    val stage = androidx.compose.runtime.remember(language) {
        androidx.compose.runtime.mutableIntStateOf(0)
    }
    androidx.compose.runtime.LaunchedEffect(language) {
        // 1 段目: UI の文言（約 330KB）は最初から出ている
        // 2 段目: シラバスの科目名まで含む中量版（約 800KB）
        // 3 段目: ユーザー入力の稀少な漢字も描ける完全版
        runCatching { Res.readBytes(contentPath) }
        stage.intValue = 1
        runCatching { Res.readBytes(fullPath) }
        stage.intValue = 2
    }
    return when (stage.intValue) {
        0 -> FontFamily(Font(ui))
        1 -> FontFamily(Font(content))
        else -> FontFamily(Font(full))
    }
}

private fun androidx.compose.ui.text.TextStyle.withFont(fontFamily: FontFamily) =
    copy(fontFamily = fontFamily, fontWeight = FontWeight.Normal)

/**
 * 画面で使うロールは 6 つに固定する。
 *
 * | ロール | 用途 |
 * | --- | --- |
 * | titleLarge 20/SemiBold | 画面名 |
 * | titleMedium 16/SemiBold | 節の見出し・行の強調 |
 * | bodyLarge 15 | 本文 |
 * | bodyMedium 14 | 行・一覧の本文 |
 * | bodySmall 12 | 補助・メタ |
 * | labelSmall 11 | さらに小さいメタ・件数 |
 *
 * 数字だけを大きく見せる箇所は headlineLarge / headlineMedium を使う。
 * 画面側で `fontSize` を直に書かない。
 */
private fun appTypography(fontFamily: FontFamily): Typography {
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.withFont(fontFamily),
        displayMedium = base.displayMedium.withFont(fontFamily),
        displaySmall = base.displaySmall.withFont(fontFamily),
        headlineLarge = base.headlineLarge.withFont(fontFamily).copy(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.withFont(fontFamily).copy(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.withFont(fontFamily).copy(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.withFont(fontFamily).copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.withFont(fontFamily).copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.withFont(fontFamily).copy(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.withFont(fontFamily).copy(fontSize = 15.sp, lineHeight = 21.sp),
        bodyMedium = base.bodyMedium.withFont(fontFamily).copy(fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = base.bodySmall.withFont(fontFamily).copy(fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = base.labelLarge.withFont(fontFamily).copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.withFont(fontFamily).copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.withFont(fontFamily).copy(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium)
    )
}

@Composable
fun OpenYaskeTheme(
    themeMode: ThemeMode,
    fontFamily: FontFamily,
    accentHue: Int = 230,
    monochrome: Boolean = false,
    content: @Composable () -> Unit
) {
    val pureBlack = themeMode == ThemeMode.PURE_BLACK
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.PURE_BLACK -> true
    }
    val base = when {
        pureBlack -> BlackAppColors
        dark -> DarkAppColors
        else -> LightAppColors
    }
    // 色相 1 つ（またはモノクロ）から primary 系を導出する。
    val palette = base.withAccent(accentHue, monochrome)
    val typography = appTypography(fontFamily)
    CompositionLocalProvider(LocalAppColors provides palette) {
        @OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
        MaterialExpressiveTheme(
            colorScheme = schemeFor(palette, dark),
            motionScheme = androidx.compose.material3.MotionScheme.expressive(),
            typography = typography,
            shapes = OpenYaskeShapes,
            content = content
        )
    }
}

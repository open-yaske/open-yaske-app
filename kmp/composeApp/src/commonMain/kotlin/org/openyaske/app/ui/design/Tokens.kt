package org.openyaske.app.ui.design

import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.pow

/**
 * 本家アプリの配色をそのまま持ってきたトークン。
 *
 * 画面の地は #EFF1FA（ごく薄い青紫）、カードは白、線は #E4E7F0。
 * 濃紺 #263F73 を主色、#5C6BC0 を差し色にする。
 * 影はほぼ使わず、1dp の線と余白で区切るのが本家の描き方。
 */
@Immutable
data class AppColors(
    val dark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val sunken: Color,
    val border: Color,
    val borderStrong: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val sectionBar: Color,
    val sectionBarText: Color,
    val navBar: Color,
    val navActive: Color,
    val danger: Color,
    val success: Color,
    val warning: Color,
    val info: Color,
    val shadow: Color
)

val LightAppColors = AppColors(
    dark = false,
    background = Color(0xFFEFF1FA),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF7F8FC),
    sunken = Color(0xFFF2F2F7),
    border = Color(0xFFE4E7F0),
    borderStrong = Color(0xFFC7CBD6),
    text = Color(0xFF111317),
    textSecondary = Color(0xFF6A7282),
    textTertiary = Color(0xFF98A0B3),
    primary = Color(0xFF263F73),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF5C6BC0),
    primaryContainer = Color(0xFFDEE4F7),
    onPrimaryContainer = Color(0xFF263F73),
    sectionBar = Color(0xFFDEE4F7),
    sectionBarText = Color(0xFF263F73),
    navBar = Color(0xFFFFFFFF),
    navActive = Color(0xFFEDEFF6),
    danger = Color(0xFFE53935),
    success = Color(0xFF43A047),
    warning = Color(0xFFFB8C00),
    info = Color(0xFF00ACC1),
    shadow = Color(0x14000000)
)

val DarkAppColors = AppColors(
    dark = true,
    background = Color(0xFF111317),
    surface = Color(0xFF1E202B),
    surfaceAlt = Color(0xFF252830),
    sunken = Color(0xFF2D323E),
    border = Color(0xFF343A4C),
    borderStrong = Color(0xFF454C63),
    text = Color(0xFFEFF1FA),
    textSecondary = Color(0xFFAAB3CA),
    textTertiary = Color(0xFF7F8798),
    primary = Color(0xFF8EA2D8),
    onPrimary = Color(0xFF111317),
    accent = Color(0xFF5C6BC0),
    primaryContainer = Color(0xFF2C3A63),
    onPrimaryContainer = Color(0xFFDEE4F7),
    sectionBar = Color(0xFF47548A),
    sectionBarText = Color(0xFFEFF1FA),
    navBar = Color(0xFF1E202B),
    navActive = Color(0xFF2C3A63),
    danger = Color(0xFFFF6B6B),
    success = Color(0xFF66BB6A),
    warning = Color(0xFFFFB74D),
    info = Color(0xFF4DD0E1),
    shadow = Color(0x33000000)
)

/** 有機 EL 向けの純黒。面は #000 に置き、境目は線と一段明るい面で作る。 */
val BlackAppColors = AppColors(
    dark = true,
    background = Color(0xFF000000),
    surface = Color(0xFF0B0B0C),
    surfaceAlt = Color(0xFF101012),
    sunken = Color(0xFF17171A),
    border = Color(0xFF26262B),
    borderStrong = Color(0xFF3A3A41),
    text = Color(0xFFF4F4F5),
    textSecondary = Color(0xFFA1A1AA),
    textTertiary = Color(0xFF6E6E76),
    primary = Color(0xFF9EC1F0),
    onPrimary = Color(0xFF001B33),
    accent = Color(0xFF8C9EFF),
    primaryContainer = Color(0xFF16233A),
    onPrimaryContainer = Color(0xFFD6E3FF),
    sectionBar = Color(0xFF1C2333),
    sectionBarText = Color(0xFFD6E3FF),
    navBar = Color(0xFF0B0B0C),
    navActive = Color(0xFF1C2333),
    danger = Color(0xFFFF7A7A),
    success = Color(0xFF6FD08C),
    warning = Color(0xFFF2B45A),
    info = Color(0xFF56C8D8),
    shadow = Color(0x00000000)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/**
 * 面は 3 種だけにする。
 *
 * - 地: [AppColors.background]
 * - 面: [AppColors.surface] + 1dp の [AppColors.border]
 * - 強調面: [AppColors.primaryContainer]
 *
 * 科目・予定の色は [tintOf]（明 16% / 暗 24%）でだけ重ねる。
 * それ以外の alpha 合成を画面側で作らない。
 */

/** 相対輝度（WCAG 2.1）。 */
private fun Color.relativeLuminance(): Double {
    fun channel(value: Float): Double {
        val v = value.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
}

/** 2 色のコントラスト比（1〜21）。 */
fun contrastRatio(a: Color, b: Color): Double {
    val la = a.relativeLuminance()
    val lb = b.relativeLuminance()
    val high = maxOf(la, lb)
    val low = minOf(la, lb)
    return (high + 0.05) / (low + 0.05)
}

/** 面に対して 4.5:1 以上を満たす側（白か黒）を選ぶ。 */
fun onColorFor(background: Color): Color {
    val white = Color(0xFFFFFFFF)
    val ink = Color(0xFF111317)
    return if (contrastRatio(background, white) >= contrastRatio(background, ink)) white else ink
}

/**
 * 色相から primary を出す。スウォッチと実際の色で同じ式を使う。
 * 彩度・明度は系統ごとに固定する（ライト L30% / ダーク L78%）。
 */
fun accentColorFor(hue: Int, dark: Boolean): Color =
    if (dark) {
        fromHsl(((hue % 360 + 360) % 360) / 360f, 0.55f, 0.78f)
    } else {
        fromHsl(((hue % 360 + 360) % 360) / 360f, 0.45f, 0.30f)
    }

/** モノクロの基準色。純白・純黒は使わない。 */
private val MonoLightPrimary = Color(0xFF3F3F46)
private val MonoLightContainer = Color(0xFFF4F4F5)
private val MonoLightOnContainer = Color(0xFF27272A)
private val MonoDarkPrimary = Color(0xFFE4E4E7)
private val MonoDarkContainer = Color(0xFF3F3F46)
private val MonoDarkContainerBlack = Color(0xFF29292E)
private val MonoDarkOnContainer = Color(0xFFF4F4F5)

/**
 * アクセントから primary 系を導出する。
 *
 * カラーは色相だけを受け取り、彩度と明度は固定する。
 * モノクロは色相を使わず、トーンの段差だけで選択状態を作る。
 * `onPrimary` はコントラスト比が大きい側を計算して選ぶ。
 */
fun AppColors.withAccent(hue: Int, monochrome: Boolean): AppColors {
    val h = ((hue % 360) + 360) % 360 / 360f
    return if (!dark) {
        val primary = if (monochrome) MonoLightPrimary else fromHsl(h, 0.45f, 0.30f)
        val container = if (monochrome) MonoLightContainer else fromHsl(h, 0.55f, 0.90f)
        val onContainer = if (monochrome) MonoLightOnContainer else fromHsl(h, 0.60f, 0.15f)
        val surfaceTone = if (monochrome) Color(0xFFF8F8F9) else fromHsl(h, 0.18f, 0.985f)
        copy(
            primary = primary,
            onPrimary = onColorFor(primary),
            primaryContainer = container,
            onPrimaryContainer = onContainer,
            sectionBar = container,
            sectionBarText = onContainer,
            navActive = if (monochrome) MonoLightContainer else fromHsl(h, 0.45f, 0.93f),
            accent = if (monochrome) MonoLightPrimary else fromHsl(h, 0.50f, 0.50f),
            background = if (monochrome) Color(0xFFF4F4F5) else fromHsl(h, 0.18f, 0.968f),
            surface = surfaceTone,
            surfaceAlt = if (monochrome) Color(0xFFFAFAFA) else fromHsl(h, 0.16f, 0.975f),
            sunken = if (monochrome) Color(0xFFEDEDEF) else fromHsl(h, 0.12f, 0.955f),
            border = if (monochrome) Color(0xFFE4E4E7) else fromHsl(h, 0.12f, 0.90f),
            borderStrong = if (monochrome) Color(0xFFC7C7CE) else fromHsl(h, 0.12f, 0.78f),
            navBar = surfaceTone
        )
    } else {
        val pureBlack = background.red == 0f && background.green == 0f && background.blue == 0f
        val primary = if (monochrome) MonoDarkPrimary else fromHsl(h, 0.55f, 0.78f)
        val container = when {
            monochrome && pureBlack -> MonoDarkContainerBlack
            monochrome -> MonoDarkContainer
            else -> fromHsl(h, 0.35f, 0.30f)
        }
        val onContainer = if (monochrome) MonoDarkOnContainer else fromHsl(h, 0.50f, 0.90f)
        val surfaceTone = if (monochrome) {
            if (pureBlack) Color(0xFF0C0C0E) else Color(0xFF1B1B1F)
        } else {
            fromHsl(h, 0.18f, if (pureBlack) 0.05f else 0.11f)
        }
        copy(
            primary = primary,
            onPrimary = onColorFor(primary),
            primaryContainer = container,
            onPrimaryContainer = onContainer,
            sectionBar = if (monochrome) container else fromHsl(h, 0.30f, 0.26f),
            sectionBarText = if (monochrome) MonoDarkOnContainer else fromHsl(h, 0.40f, 0.92f),
            navActive = if (monochrome) Color(0xFF3F3F46) else fromHsl(h, 0.45f, 0.24f),
            accent = if (monochrome) Color(0xFFD4D4D8) else fromHsl(h, 0.70f, 0.68f),
            background = if (pureBlack) background else if (monochrome) Color(0xFF141417) else fromHsl(h, 0.20f, 0.065f),
            surface = surfaceTone,
            surfaceAlt = if (monochrome) {
                if (pureBlack) Color(0xFF141417) else Color(0xFF232327)
            } else {
                fromHsl(h, 0.16f, if (pureBlack) 0.08f else 0.145f)
            },
            sunken = if (monochrome) Color(0xFF2A2A2F) else fromHsl(h, 0.14f, if (pureBlack) 0.11f else 0.175f),
            border = if (monochrome) Color(0xFF3A3A41) else fromHsl(h, 0.15f, if (pureBlack) 0.19f else 0.21f),
            borderStrong = if (monochrome) Color(0xFF52525B) else fromHsl(h, 0.15f, 0.30f),
            navBar = surfaceTone
        )
    }
}

/** 設定に出す色相のプリセット（赤 / 橙 / 黄 / 緑 / 青緑 / 青 / 藍 / 紫 / ピンク）。 */
val AccentHuePresets = listOf(0, 25, 45, 140, 175, 210, 230, 265, 330)

/** `#RRGGBB` に戻す。 */
fun Color.toAccentHex(): String {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return "#" + listOf(r, g, b).joinToString("") { it.toString(16).padStart(2, '0').uppercase() }
}

private fun Color.toHsl(): Triple<Float, Float, Float> {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val lightness = (max + min) / 2f
    if (max == min) return Triple(0f, 0f, lightness)
    val delta = max - min
    val saturation = if (lightness > 0.5f) delta / (2f - max - min) else delta / (max + min)
    val hue = when (max) {
        r -> ((g - b) / delta + if (g < b) 6f else 0f)
        g -> ((b - r) / delta + 2f)
        else -> ((r - g) / delta + 4f)
    } / 6f
    return Triple(hue, saturation, lightness)
}

private fun fromHsl(hue: Float, saturation: Float, lightness: Float): Color {
    fun hueToRgb(p: Float, q: Float, t0: Float): Float {
        var t = t0
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        return when {
            t < 1f / 6f -> p + (q - p) * 6f * t
            t < 1f / 2f -> q
            t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
            else -> p
        }
    }
    val q = if (lightness < 0.5f) lightness * (1f + saturation)
    else lightness + saturation - lightness * saturation
    val p = 2f * lightness - q
    return Color(
        red = hueToRgb(p, q, hue + 1f / 3f).coerceIn(0f, 1f),
        green = hueToRgb(p, q, hue).coerceIn(0f, 1f),
        blue = hueToRgb(p, q, hue - 1f / 3f).coerceIn(0f, 1f)
    )
}

/** 科目・予定に使う色。本家のカレンダーと同じ、彩度の高い 14 色。 */
val CoursePalette = listOf(
    Color(0xFFE53935),
    Color(0xFFD81B60),
    Color(0xFF8E24AA),
    Color(0xFF5E35B1),
    Color(0xFF3949AB),
    Color(0xFF1E88E5),
    Color(0xFF00ACC1),
    Color(0xFF00897B),
    Color(0xFF43A047),
    Color(0xFF7CB342),
    Color(0xFFFB8C00),
    Color(0xFFF4511E),
    Color(0xFF6D4C41),
    Color(0xFF546E7A)
)

/** 本家の既定色 `custom:#007aff` も含めて、文字列から色へ戻す。 */
fun parseCourseColor(token: String?, index: Int = 0): Color {
    if (token.isNullOrBlank()) return CoursePalette[index.mod(CoursePalette.size)]
    val hex = token.removePrefix("custom:").removePrefix("#")
    return runCatching {
        if (hex.length == 8) Color(hex.toLong(16))
        else Color(0xFF000000 or hex.toLong(16))
    }.getOrElse { CoursePalette[index.mod(CoursePalette.size)] }
}

/** 面の色に対して読める文字色を選ぶ。 */
fun readableOn(color: Color): Color {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return if (luminance > 0.45f) Color(0xFF111317) else Color(0xFFFFFFFF)
}

/** 塗りつぶしに使う淡い面。科目色はこの 2 値だけ（明 16% / 暗 24%）。 */
fun tintOf(color: Color, light: Boolean): Color =
    if (light) color.copy(alpha = 0.16f) else color.copy(alpha = 0.24f)

object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
}

/**
 * 角丸は 4 段だけにする。
 *
 * - `card` 12dp: カード・リスト面
 * - `control` 16dp: セル・チップ・ボタン・入力欄
 * - `sheet` 28dp: ヒーロー・シート
 * - `pill` 50%: 細いバー・丸ボタン（[AppShapes.pill]）
 *
 * 画面に `RoundedCornerShape(14.dp)` のような直値を書かない。
 * clip と border には必ず同じ Shape 変数を渡す（[AppShapes]）。
 */
object Radius {
    /** 4dp。行の角、細い帯。 */
    val xs = 4.dp
    val card = 12.dp
    val control = 16.dp
    val sheet = 28.dp
}

/** [Radius] から作る唯一の Shape 群。clip・background・border で共有する。 */
object AppShapes {
    val xs = androidx.compose.foundation.shape.RoundedCornerShape(Radius.xs)
    val control = androidx.compose.foundation.shape.RoundedCornerShape(Radius.control)
    val card = androidx.compose.foundation.shape.RoundedCornerShape(Radius.card)
    val sheet = androidx.compose.foundation.shape.RoundedCornerShape(Radius.sheet)
    val pill = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)

    /** ヒーローだけ非対称にする（左上 28・左下 12）。 */
    val hero = androidx.compose.foundation.shape.RoundedCornerShape(
        topStart = Radius.sheet,
        bottomStart = Radius.card,
        topEnd = Radius.sheet,
        bottomEnd = Radius.sheet
    )

    /** 4dp ↔ 16dp のように段の間で角を動かすとき用。 */
    fun morph(radius: androidx.compose.ui.unit.Dp) =
        androidx.compose.foundation.shape.RoundedCornerShape(radius)
}

/**
 * 高さ 480dp 未満の横画面。ヘッダー・下部余白・列数をまとめて詰める。
 * `App` の [androidx.compose.foundation.layout.BoxWithConstraints] で切り替える。
 */
val LocalCompactLayout = staticCompositionLocalOf { false }

/** 画面の余白とグリッド。全画面で同じ値を使う。 */
object Layout {
    /** 画面の左右余白。 */
    val gutter = 16.dp

    /** 時間割・カレンダーのセル間隔（片側）。 */
    val cellGap = 2.dp

    /** 下部ナビ（80dp）に 16dp を足した、画面下の余白。 */
    val bottomInset = 96.dp

    /** 横画面コンパクト時の画面下の余白。 */
    val bottomInsetCompact = 72.dp
}

/**
 * 画面下の余白。下部ナビがあるときは 96dp、横画面コンパクトでは 72dp。
 * 画面ごとに 88/110/150 のような値を書かない。
 */
@androidx.compose.runtime.Composable
fun screenBottomPadding(bottomInset: Boolean): androidx.compose.ui.unit.Dp = when {
    !bottomInset -> Space.xxl
    else -> {
        // edge-to-edge なので、ナビゲーションバーの実寸も足す。
        val systemBars = androidx.compose.foundation.layout.WindowInsets.navigationBars
            .asPaddingValues()
            .calculateBottomPadding()
        if (LocalCompactLayout.current) Layout.bottomInsetCompact + systemBars
        else Layout.bottomInset + systemBars
    }
}

object Sizes {
    /** タブ画面のヘッダー高さ。横画面（コンパクト）は 44dp。 */
    val headerHeight = 56.dp
    val headerCompact = 44.dp

    /** アプリバーの左右スロット。右はこの倍数で置く。 */
    val appBarSlot = 40.dp
    val iconButton = 40.dp
    val iconButtonLarge = 44.dp
    val navBarHeight = 80.dp
    val navBarCompact = 48.dp
    val fab = 56.dp
    val rowHeight = 56.dp
    val chipHeight = 36.dp
    val fieldHeight = 46.dp
}

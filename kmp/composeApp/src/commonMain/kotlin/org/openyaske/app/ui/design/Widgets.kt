package org.openyaske.app.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 面。白 + 1dp の線 + 16dp 角丸。影は使わない。 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = LocalAppColors.current.surface,
    shape: Shape = AppShapes.card,
    border: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color, shape)
            .then(if (border) Modifier.border(1.dp, colors.border, shape) else Modifier),
        content = content
    )
}

/**
 * 見出しは文字だけで示す。帯も線も置かない。
 * 余白（上 24dp / 下 8dp）で区切り、色は補助文字に落とす。
 */
@Composable
fun SectionBar(text: String, modifier: Modifier = Modifier, trailing: (@Composable RowScope.() -> Unit)? = null) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        trailing?.invoke(this)
    }
}

/** 淡い円の上に乗るアイコンボタン。本家のヘッダーはこれ。 */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.iconButtonLarge,
    container: Color = LocalAppColors.current.sunken,
    tint: Color = LocalAppColors.current.text
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** 塗りの主ボタン。「追加」「保存」に使う。角丸は control に揃える。 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 40.dp
) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .height(height)
            .defaultMinSize(minWidth = 64.dp)
            .clip(AppShapes.control)
            .background(if (enabled) colors.primary else colors.borderStrong)
            .clickable(enabled = enabled) {
                tapFeedback()
                onClick()
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = colors.onPrimary,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}

/** 検索欄やフィルタの下地。本家は #F2F2F7 の角丸。 */
@Composable
fun SoftField(
    modifier: Modifier = Modifier,
    height: Dp = Sizes.fieldHeight,
    content: @Composable RowScope.() -> Unit
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .height(height)
            .clip(AppShapes.control)
            .background(colors.sunken)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** 押すと開くチップ。本家の「前期˅」「月曜˅」に相当する。 */
@Composable
fun FilterPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    trailing: ImageVector? = null,
    fontFamily: FontFamily? = null,
    height: Dp = Sizes.chipHeight
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .height(height)
            .clip(AppShapes.control)
            .background(if (selected) colors.primaryContainer else colors.sunken)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = fontFamily,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) colors.onPrimaryContainer else colors.textSecondary
        )
        trailing?.let {
            Icon(it, null, tint = if (selected) colors.onPrimaryContainer else colors.textSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

/** 行の右端に置く開示の矢印。文字の「›」は使わない。 */
@Composable
fun Disclosure(modifier: Modifier = Modifier, tint: androidx.compose.ui.graphics.Color = LocalAppColors.current.textTertiary) {
    Icon(
        imageVector = AppIcons.disclosure,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(18.dp)
    )
}

/** 縦の 1dp 区切り線。 */
@Composable
fun VerticalHairLine(modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = LocalAppColors.current.border) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(color)
    )
}

/** 1dp の区切り線。 */
@Composable
fun HairLine(modifier: Modifier = Modifier, color: Color = LocalAppColors.current.border) {
    Box(modifier = modifier.fillMaxWidth().height(1.dp).background(color))
}

/** アイコン + 見出し + 補足 + 右端の矢印。設定やツール一覧の行。 */
@Composable
fun NavRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailingText: String? = null,
    showChevron: Boolean = true
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.text)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, lineHeight = 16.sp)
            }
        }
        trailingText?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
        if (showChevron) {
            Disclosure()
        }
    }
}

/** 角丸のアイコンタイル。ホームの「授業関連」グリッド用。 */
@Composable
fun IconTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalAppColors.current.accent
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .clip(AppShapes.card)
            .background(colors.surfaceAlt)
            .border(1.dp, colors.border, AppShapes.card)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 13.sp
        )
    }
}

package org.openyaske.app.ui.design

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * アプリバーは 2 種だけにする。
 *
 * - [AppTopBar]: 下位画面（戻るがある）。左に戻る、中央に題、右に操作。
 * - [TabHeader]: タブ画面。左 40dp スロット、中央は中央寄せ、右は 40dp の倍数。
 *
 * 高さは縦 56dp / 横 44dp（[LocalCompactLayout]）で固定し、
 * 画面ごとに高さや左右のバランスを変えない。
 */

/** 下位画面のアプリバー。題は中央、戻るは左、操作は右。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = LocalAppColors.current
    val compact = LocalCompactLayout.current
    CenterAlignedTopAppBar(
        modifier = modifier
            .statusBarsPadding()
            .height(if (compact) Sizes.headerCompact else Sizes.headerHeight),
        windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(AppIcons.back, contentDescription = null, tint = colors.text)
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.background,
            titleContentColor = colors.text,
            navigationIconContentColor = colors.text,
            actionIconContentColor = colors.text
        )
    )
}

/**
 * タブ画面のヘッダー。
 *
 * 中央は画面幅に対して幾何学的に中央へ置く（左右の要素数でずれない）。
 * 左は 40dp のスロット、右は 40dp の倍数に揃える。
 *
 * @param leading 左スロット（戻る・前月など）
 * @param tabs 中央左寄せの帯（3 つのタブなど）。title とは併用しない
 * @param title 中央寄せの題
 * @param actions 右スロット
 */
@Composable
fun TabHeader(
    modifier: Modifier = Modifier,
    compact: Boolean = LocalCompactLayout.current,
    leading: (@Composable () -> Unit)? = null,
    tabs: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(if (compact) Sizes.headerCompact else Sizes.headerHeight)
            .padding(horizontal = Layout.gutter),
        contentAlignment = Alignment.Center
    ) {
        tabs?.let {
            Box(
                modifier = Modifier.fillMaxWidth().padding(end = Sizes.appBarSlot),
                contentAlignment = Alignment.CenterStart
            ) { it() }
        }
        title?.let {
            // 左右のスロットに重ならない幅で中央に置く。
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Sizes.appBarSlot),
                contentAlignment = Alignment.Center
            ) { it() }
        }
        leading?.let {
            Box(
                modifier = Modifier.align(Alignment.CenterStart).widthIn(min = Sizes.appBarSlot).height(Sizes.appBarSlot),
                contentAlignment = Alignment.CenterStart
            ) { it() }
        }
        actions?.let {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd).height(Sizes.appBarSlot),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.xs)
            ) { it() }
        }
    }
}

/** アプリバーに置くアイコンボタン。40dp のスロットに収める。 */
@Composable
fun AppBarIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    filled: Boolean = false
) {
    val colors = LocalAppColors.current
    if (filled) {
        androidx.compose.material3.FilledIconButton(
            onClick = onClick,
            modifier = Modifier.height(32.dp).widthIn(min = 32.dp),
            colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary
            )
        ) {
            Icon(icon, contentDescription, modifier = Modifier.size(18.dp))
        }
    } else {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription, tint = colors.text, modifier = Modifier.size(22.dp))
        }
    }
}

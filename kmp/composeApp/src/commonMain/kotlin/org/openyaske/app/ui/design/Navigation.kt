package org.openyaske.app.ui.design

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openyaske.app.ui.Destination
import org.openyaske.app.ui.LocalUiStrings

/**
 * 画面下部のナビゲーション。M3 の NavigationBar をそのまま使い、
 * 選択項目にはピル型のインジケータを出す（iOS 風の浮遊ピルはやめた）。
 */
@Composable
fun AppNavigationBar(
    destination: Destination,
    onDestination: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    val compact = LocalCompactLayout.current
    Column(modifier = modifier.fillMaxWidth()) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
    // insets は既定（システムバーを消費する）に任せる。高さも固定しない。
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = colors.navBar,
        contentColor = colors.text,
        tonalElevation = 0.dp
    ) {
        Destination.entries.forEach { item ->
            val selected = item == destination
            NavigationBarItem(
                selected = selected,
                onClick = {
                    selectFeedback()
                    onDestination(item)
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = t[item.labelKey],
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = if (compact) null else ({ 
                    Text(
                        text = t[item.labelKey],
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }),
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    selectedTextColor = colors.primary,
                    indicatorColor = colors.primaryContainer,
                    unselectedIconColor = colors.textSecondary,
                    unselectedTextColor = colors.textSecondary
                )
            )
        }
    }
    }
}

/** 幅が広いときの縦レール。 */
@Composable
fun SideRail(
    destination: Destination,
    onDestination: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(88.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Destination.entries.forEach { item ->
            val selected = item == destination
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppShapes.card)
                    .then(if (selected) Modifier.background(colors.navActive) else Modifier)
                    .clickable {
                        selectFeedback()
                        onDestination(item)
                    }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    item.icon,
                    t[item.labelKey],
                    tint = if (selected) colors.primary else colors.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    t[item.labelKey],
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) colors.primary else colors.textSecondary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

/** 小さい丸バッジ。件数や未読に使う。 */
@Composable
fun CountBadge(text: String, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.sunken)
            .padding(horizontal = 6.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
    }
}

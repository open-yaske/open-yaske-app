package org.openyaske.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.resources.painterResource
import org.openyaske.app.resources.Res
import org.openyaske.app.resources.app_icon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openyaske.app.ui.design.screenBottomPadding
import org.openyaske.app.ui.design.AppCard
import org.openyaske.app.ui.design.AppIcons
import org.openyaske.app.ui.design.Disclosure
import org.openyaske.app.ui.design.HairLine
import org.openyaske.app.ui.design.Layout
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.SectionBar
import org.openyaske.app.ui.design.Space
import org.openyaske.core.api.CoreApi

/**
 * 下部ナビの「その他」。ナビに入りきらない機能をここに集める。
 * 行き先は増えてもよく、増えた分はこの 1 枚に収まる。
 */
@Composable
fun MoreScreen(
    api: CoreApi,
    onSearch: () -> Unit,
    onPdf: () -> Unit,
    onGpa: () -> Unit,
    onSettings: () -> Unit,
    onLicenses: () -> Unit,
    bottomInset: Boolean = true
) {
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        org.openyaske.app.ui.design.TabHeader(
            title = {
                Text(
                    text = t["more_title"],
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.text
                )
            }
        )
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxHeight(),
            contentPadding = PaddingValues(
                start = Layout.gutter,
                end = Layout.gutter,
                bottom = screenBottomPadding(bottomInset)
            ),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            item { SectionBar(t["more_tools"]) }
            item {
                AppCard {
                    Column {
                        MoreRow(
                            icon = AppIcons.search,
                            title = t["more_search"],
                            subtitle = null,
                            onClick = onSearch
                        )

                        HairLine(modifier = Modifier.padding(start = 52.dp))
                        MoreRow(
                            icon = AppIcons.pdf,
                            title = t["more_pdf"],
                            subtitle = null,
                            onClick = onPdf
                        )
                        HairLine(modifier = Modifier.padding(start = 52.dp))
                        MoreRow(
                            icon = AppIcons.grades,
                            title = t["gpa_title"],
                            subtitle = null,
                            onClick = onGpa
                        )
                    }
                }
            }

            item { SectionBar(t["settings"]) }
            item {
                AppCard {
                    MoreRow(
                        icon = AppIcons.settings,
                        title = t["settings"],
                        subtitle = null,
                        onClick = onSettings
                    )
                    HairLine(modifier = Modifier.padding(start = 52.dp))
                    MoreRow(
                        icon = AppIcons.license,
                        title = t["more_licenses"],
                        subtitle = null,
                        onClick = onLicenses
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Space.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Image(
                        painter = org.jetbrains.compose.resources.painterResource(Res.drawable.app_icon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(org.openyaske.app.ui.design.AppShapes.control)
                    )
                    Column(modifier = Modifier.padding(start = Space.sm)) {
                        Text(
                            text = "OpenYASKE",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary
                        )
                        Text(
                            text = t["settings_version"],
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    androidx.compose.material3.ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyMedium, color = colors.text) },
        supportingContent = subtitle?.let { text ->
            { Text(text, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary) }
        },
        leadingContent = {
            Icon(icon, null, tint = colors.accent, modifier = Modifier.size(20.dp))
        },
        trailingContent = { Disclosure() },
        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = colors.surface)
    )
}

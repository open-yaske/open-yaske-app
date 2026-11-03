package org.openyaske.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openyaske.app.ui.design.AppIcons
import org.openyaske.app.ui.design.AppCard
import org.openyaske.app.ui.design.tintOf
import org.openyaske.app.ui.design.AppShapes
import org.openyaske.app.ui.design.HairLine
import org.openyaske.app.ui.design.Disclosure
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.Radius
import org.openyaske.app.ui.design.Space

/** 本家の APK に入っていた学内リンク。URL はそのまま使う。 */
data class CampusLink(
    val title: String,
    val url: String,
    val icon: ImageVector
)

data class CampusLinkGroup(
    val title: String,
    val links: List<CampusLink>
)

val CampusLinkGroups = listOf(
    CampusLinkGroup(
        title = "よく使う",
        links = listOf(
            CampusLink("UECポータル", "https://portalweb.uec.ac.jp/Portal/", Icons.Filled.AccountCircle),
            CampusLink("学務情報", "https://campusweb.office.uec.ac.jp/campusweb/", Icons.Filled.School),
            CampusLink("Classroom", "https://classroom.google.com", Icons.Filled.Cloud),
            CampusLink("WebClass", "https://webclass.cdel.uec.ac.jp/webclass/login.php", Icons.AutoMirrored.Filled.MenuBook)
        )
    ),
    CampusLinkGroup(
        title = "授業ツール",
        links = listOf(
            CampusLink("JB Moodle", "https://joho.g-edu.uec.ac.jp/moodle3/login/index.php", Icons.AutoMirrored.Filled.MenuBook),
            CampusLink("Slash 9", "https://s9.logs.world/auth/signin?redirect=%2F", Icons.Filled.Terminal),
            CampusLink("シラバス", "https://kyoumu.office.uec.ac.jp/syllabus/", Icons.Filled.Search),
            CampusLink("学修要覧", "https://kyoumu.office.uec.ac.jp/youran/youran.html", Icons.AutoMirrored.Filled.MenuBook)
        )
    ),
    CampusLinkGroup(
        title = "キャンパス",
        links = listOf(
            CampusLink("図書館", "https://www.lib.uec.ac.jp", Icons.AutoMirrored.Filled.MenuBook),
            CampusLink("学内マップ", "https://uec-map.e-chan.me", Icons.Filled.Map),
            CampusLink("ラボガイド", "https://www.uec.ac.jp/arc/laboguide.html", Icons.Filled.Public),
            CampusLink("保健管理センター", "https://www.health.uec.ac.jp/", Icons.Filled.HealthAndSafety),
            CampusLink("学生相談室", "https://www.uec.ac.jp/campus/desk/consultation/", Icons.AutoMirrored.Filled.Help),
            CampusLink("ITCポータル", "https://www.cc.uec.ac.jp", Icons.Filled.Terminal)
        )
    ),
    CampusLinkGroup(
        title = "生活・進路",
        links = listOf(
            CampusLink("奨学金", "https://www.uec.ac.jp/campus/fee/scholarship.html", Icons.Filled.Payments),
            CampusLink("学生寮", "https://www.uec.ac.jp/campus/welfare/dormitory.html", Icons.Filled.Home),
            CampusLink("就職・キャリア", "https://www.uec.ac.jp/campus/career/", Icons.Filled.Work),
            CampusLink("留学", "https://www.uec.ac.jp/campus/exchange/", Icons.Filled.Public),
            CampusLink("休学・退学等", "https://www.uec.ac.jp/campus/academic/absence.html", Icons.AutoMirrored.Filled.Help),
            CampusLink("学生生活", "https://www.uec.ac.jp/campus/", Icons.Filled.AccountCircle)
        )
    )
)

/**
 * 学内リンク。1 枚の面の中に、見出しと行を積む。
 * タイルを並べるより、行のほうが読みやすく、面の数も増えない。
 */
@Composable
fun CampusLinksSection(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val uriHandler = LocalUriHandler.current
    var expanded by remember { mutableStateOf(setOf(CampusLinkGroups.first().title)) }

    // グループごとにアイコンの色を変えて、行の並びに表情を出す。
    val groupColors = listOf(
        LocalAppColors.current.primary,
        LocalAppColors.current.info,
        LocalAppColors.current.success,
        LocalAppColors.current.warning
    )
    AppCard(modifier = modifier.fillMaxWidth()) {
        CampusLinkGroups.forEachIndexed { groupIndex, group ->
            if (groupIndex > 0) HairLine()
            val open = group.title in expanded
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expanded = if (open) expanded - group.title else expanded + group.title
                    }
                    .padding(horizontal = Space.lg, vertical = Space.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (open) AppIcons.collapse else AppIcons.expand,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (open) {
                val tint = groupColors[groupIndex % groupColors.size]
                group.links.forEach { link ->
                    HairLine(modifier = Modifier.padding(start = 60.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { runCatching { uriHandler.openUri(link.url) } }
                            .padding(horizontal = Space.lg, vertical = Space.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(AppShapes.card)
                                .background(tintOf(tint, !colors.dark)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                link.icon,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = link.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.text,
                            modifier = Modifier.weight(1f).padding(start = Space.md)
                        )
                        Disclosure()
                    }
                }
            }
        }
    }
}

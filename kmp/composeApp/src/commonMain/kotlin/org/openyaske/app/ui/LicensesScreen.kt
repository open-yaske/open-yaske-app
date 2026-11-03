package org.openyaske.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.openyaske.app.resources.Res
import org.openyaske.app.ui.design.AppCard
import org.openyaske.app.ui.design.AppTopBar
import org.openyaske.app.ui.design.Layout
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.SectionBar
import org.openyaske.app.ui.design.Space

/** 同梱物のライセンス。外部ライブラリの全文は同梱せず、出どころと種類を明記する。 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    var notice by remember { mutableStateOf<String?>(null) }
    var ofl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        notice = runCatching { Res.readBytes("files/licenses/notice.txt").decodeToString() }.getOrNull()
        ofl = runCatching { Res.readBytes("files/licenses/OFL-1.1.txt").decodeToString() }.getOrNull()
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        AppTopBar(title = t["more_licenses"], onBack = onBack, modifier = Modifier.widthIn(max = 760.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(
                start = Layout.gutter,
                end = Layout.gutter,
                bottom = Space.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            item {
                AppCard {
                    Text(
                        text = notice ?: t["common_loading"],
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp,
                        color = colors.text,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            item { SectionBar("SIL Open Font License 1.1") }
            item {
                AppCard {
                    Text(
                        text = ofl ?: t["common_loading"],
                        style = MaterialTheme.typography.labelSmall,
                        lineHeight = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            item { Spacer(Modifier.height(Space.lg)) }
        }
    }
}

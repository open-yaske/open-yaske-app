package org.openyaske.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openyaske.app.media.PdfExportMode
import org.openyaske.app.media.PdfExportState
import org.openyaske.app.media.pdfExportState
import org.openyaske.app.media.pdfExportSupported
import org.openyaske.app.media.pdfScanSupported
import org.openyaske.app.media.startPdfExport
import org.openyaske.app.media.startPdfScan
import org.openyaske.app.ui.design.AppCard
import org.openyaske.app.ui.design.AppShapes
import org.openyaske.app.ui.design.AppTopBar
import org.openyaske.app.ui.design.Layout
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.SectionBar
import org.openyaske.app.ui.design.Space
import org.openyaske.app.ui.design.tapFeedback

/**
 * 書類を PDF にする画面。入口は 2 つ。
 * - 書類をスキャン: 端末のスキャナ（Android は ML Kit Document Scanner、iOS は VisionKit）を開く。
 * - 写真を選ぶ: アルバムの画像をそのままページにする。
 * 圧縮は「目標容量」で選ぶ。縮小と書き出しは各プラットフォームが持つ。
 */
@Composable
fun PdfScreen(onBack: () -> Unit) {
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    val supported = remember { pdfExportSupported() }
    val scanner = remember { pdfScanSupported() }
    var mode by remember { mutableStateOf(PdfExportMode.STANDARD) }
    var state by remember { mutableStateOf<PdfExportState>(PdfExportState.Idle) }

    // 書き出しは非同期なので、画面が開いている間は状態を見に行く。
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            state = pdfExportState()
            kotlinx.coroutines.delay(500)
        }
    }

    val behavior = androidx.compose.material3.TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        androidx.compose.material3.rememberTopAppBarState()
    )
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        AppTopBar(title = t["more_pdf"], onBack = onBack)
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxHeight()
                .nestedScroll(behavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = Layout.gutter,
                end = Layout.gutter,
                bottom = Space.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            item { SectionBar(t["pdf_source"]) }
            item {
                SourceButton(
                    title = t["pdf_scan"],
                    hint = if (scanner) t["pdf_scan_hint"] else t["pdf_scan_unavailable"],
                    primary = true,
                    enabled = scanner,
                    onClick = {
                        tapFeedback()
                        startPdfScan(mode)
                        state = pdfExportState()
                    }
                )
            }
            item {
                SourceButton(
                    title = t["pdf_pick"],
                    hint = t["pdf_pick_hint"],
                    primary = false,
                    enabled = supported,
                    onClick = {
                        tapFeedback()
                        startPdfExport(mode)
                        state = pdfExportState()
                    }
                )
            }
            item { SectionBar(t["pdf_compress"]) }
            item {
                Text(
                    text = t["pdf_multi_hint"],
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
            item {
                AppCard {
                    Column {
                        PdfExportMode.entries.forEachIndexed { index, item ->
                            if (index > 0) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(colors.border)
                                )
                            }
                            val selected = item == mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { mode = item }
                                    .background(if (selected) colors.primaryContainer else colors.surface)
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = t[item.labelKey],
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selected) colors.onPrimaryContainer else colors.text
                                    )
                                    Text(
                                        text = item.hint(t),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                }
                                if (selected) {
                                    Text(
                                        "✓",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(Space.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state is PdfExportState.Working) {
                        androidx.compose.material3.LoadingIndicator(
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(Space.sm))
                    }
                    Text(
                        text = state.message(t),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceButton(
    title: String,
    hint: String,
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val background = when {
        !enabled -> colors.sunken
        primary -> colors.primary
        else -> colors.surface
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Space.md, vertical = Space.md),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = when {
                !enabled -> colors.textTertiary
                primary -> colors.onPrimary
                else -> colors.text
            }
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.labelSmall,
            color = when {
                !enabled -> colors.textTertiary
                primary -> colors.onPrimary.copy(alpha = 0.82f)
                else -> colors.textSecondary
            }
        )
    }
}

private fun PdfExportState.message(t: org.openyaske.app.ui.UiStrings): String = when (this) {
    PdfExportState.Idle -> ""
    is PdfExportState.Working -> t["pdf_working"]
    is PdfExportState.Done -> "${t["pdf_done"]} (${sizeBytes / 1024} KB)"
    is PdfExportState.Cancelled -> t["pdf_cancelled"]
    is PdfExportState.Failed -> text
}

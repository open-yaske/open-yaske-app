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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openyaske.app.media.PdfExportMode
import org.openyaske.app.media.PdfExportState
import org.openyaske.app.media.pdfExportState
import org.openyaske.app.media.pdfExportSupported
import org.openyaske.app.media.pdfCaptureSupported
import org.openyaske.app.media.startPdfCapture
import org.openyaske.app.media.startPdfExport
import org.openyaske.app.ui.design.AppCard
import org.openyaske.app.ui.design.AppShapes
import org.openyaske.app.ui.design.AppTopBar
import org.openyaske.app.ui.design.Layout
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.SectionBar
import org.openyaske.app.ui.design.Space

/**
 * 写真を 1 つの PDF にまとめる画面。
 * 圧縮は「目標容量」で選ぶ。実際の縮小と書き出しは各プラットフォームが持つ。
 */
@Composable
fun PdfScreen(onBack: () -> Unit) {
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    val supported = remember { pdfExportSupported() }
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
        item {
            SectionBar(t["pdf_compress"])
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
                                Text("✓", style = MaterialTheme.typography.titleMedium, color = colors.primary)
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(Space.md))
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.pill)
                        .background(if (supported) colors.primary else colors.sunken)
                        .clickable(enabled = supported) {
                            org.openyaske.app.ui.design.tapFeedback()
                            startPdfExport(mode)
                            state = pdfExportState()
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (supported) t["pdf_start"] else t["pdf_unavailable"],
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (supported) colors.onPrimary else colors.textTertiary
                    )
                }
                val camera = remember { pdfCaptureSupported() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.pill)
                        .background(if (camera) colors.primaryContainer else colors.sunken)
                        .clickable(enabled = camera) {
                            org.openyaske.app.ui.design.tapFeedback()
                            startPdfCapture(mode)
                            state = pdfExportState()
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (camera) t["pdf_capture"] else t["pdf_capture_unavailable"],
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (camera) colors.onPrimaryContainer else colors.textTertiary
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Space.md),
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

private fun PdfExportState.message(t: org.openyaske.app.ui.UiStrings): String = when (this) {
    PdfExportState.Idle -> ""
    is PdfExportState.Working -> t["pdf_working"]
    is PdfExportState.Done -> "${t["pdf_done"]} (${sizeBytes / 1024} KB)"
    is PdfExportState.Cancelled -> t["pdf_cancelled"]
    is PdfExportState.Failed -> text
}

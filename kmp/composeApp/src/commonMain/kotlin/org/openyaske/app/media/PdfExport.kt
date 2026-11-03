package org.openyaske.app.media

import org.openyaske.app.ui.UiStrings

/**
 * 圧縮モード。宣言順がそのまま画面の順で、目標容量の小さい順に並べる。
 * 無圧縮だけは容量を狙わないので末尾に置く。
 */
enum class PdfExportMode(val labelKey: String, val targetMb: Int) {
    /** 1MB 目安。グレースケール + 2 値化 + 1000px。文字の可読性を優先する。 */
    EXTREME("pdf_mode_extreme", 1),
    LIGHT("pdf_mode_light", 2),
    GRAY("pdf_mode_gray", 3),
    STANDARD("pdf_mode_standard", 5),
    HIGH("pdf_mode_high", 10),

    /** 再エンコードしない。JPEG はそのまま、それ以外は最高品質で JPEG にする。 */
    ORIGINAL("pdf_mode_original", 0);

    /** ラベルが容量を示すので、説明だけを返す。 */
    fun hint(t: UiStrings): String = t["pdf_mode_${name.lowercase()}_hint"]
}

sealed interface PdfExportState {
    data object Idle : PdfExportState
    data object Working : PdfExportState
    data class Done(val sizeBytes: Int) : PdfExportState
    data object Cancelled : PdfExportState
    data class Failed(val text: String) : PdfExportState
}

expect fun pdfExportSupported(): Boolean

expect fun startPdfExport(mode: PdfExportMode)

expect fun pdfExportState(): PdfExportState

/** その場で撮影して 1 枚から作る。 */
expect fun pdfCaptureSupported(): Boolean

expect fun startPdfCapture(mode: PdfExportMode)

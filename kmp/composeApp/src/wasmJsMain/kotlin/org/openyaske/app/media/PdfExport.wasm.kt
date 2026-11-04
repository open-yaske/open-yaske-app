@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.openyaske.app.media

actual fun pdfExportSupported(): Boolean = true

actual fun startPdfExport(mode: PdfExportMode) {
    runCatching {
    when (mode) {
        PdfExportMode.LIGHT -> exportLight()
        PdfExportMode.STANDARD -> exportStandard()
        PdfExportMode.HIGH -> exportHigh()
        PdfExportMode.GRAY -> exportGray()
        PdfExportMode.EXTREME -> exportExtreme()
        PdfExportMode.ORIGINAL -> exportOriginal()
    }
    }
}

// js() はトップレベル関数の式本体でしか使えないため、呼び出しごとに分ける。
private fun exportLight(): Unit = js("window.AppMedia.exportLight()")
private fun exportStandard(): Unit = js("window.AppMedia.exportStandard()")
private fun exportHigh(): Unit = js("window.AppMedia.exportHigh()")
private fun exportGray(): Unit = js("window.AppMedia.exportGray()")
private fun exportExtreme(): Unit = js("window.AppMedia.exportExtreme()")
private fun exportOriginal(): Unit = js("window.AppMedia.exportOriginal()")
private fun exportStateJson(): String = js("window.AppMedia.state()")

actual fun pdfExportState(): PdfExportState {
    val json: String = runCatching { exportStateJson() }.getOrElse { return PdfExportState.Idle }
    val status = json.substringAfter("\"status\":\"", "").substringBefore("\"")
    val message = json.substringAfter("\"message\":\"", "").substringBefore("\"")
    val bytes = json.substringAfter("\"bytes\":", "0").substringBefore("}").trim().toIntOrNull() ?: 0
    return when (status) {
        "working" -> PdfExportState.Working
        "done" -> PdfExportState.Done(bytes)
        "cancelled" -> PdfExportState.Cancelled
        "error" -> PdfExportState.Failed(message)
        else -> PdfExportState.Idle
    }
}

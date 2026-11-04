package org.openyaske.app.media

actual fun pdfExportSupported(): Boolean = true

actual fun startPdfExport(mode: PdfExportMode) = AndroidPdfBridge.start(mode)

actual fun pdfExportState(): PdfExportState = AndroidPdfBridge.state()

actual fun pdfScanSupported(): Boolean = AndroidPdfBridge.scanSupported()

actual fun startPdfScan(mode: PdfExportMode) = AndroidPdfBridge.startScan(mode)

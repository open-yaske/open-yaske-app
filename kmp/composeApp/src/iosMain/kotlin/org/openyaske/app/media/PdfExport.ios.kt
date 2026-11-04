@file:OptIn(ExperimentalForeignApi::class)

package org.openyaske.app.media

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIViewController
import platform.VisionKit.VNDocumentCameraScan
import platform.VisionKit.VNDocumentCameraViewController
import platform.VisionKit.VNDocumentCameraViewControllerDelegateProtocol
import platform.darwin.NSObject
import kotlin.concurrent.Volatile
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import org.openyaske.core.media.PdfBuilder
import org.openyaske.core.media.PdfImage
import org.openyaske.core.media.PdfPageSize

actual fun pdfExportSupported(): Boolean = true

actual fun pdfExportState(): PdfExportState = IosPdf.state

actual fun startPdfExport(mode: PdfExportMode) = IosPdf.pick(mode)

actual fun pdfScanSupported(): Boolean = true

actual fun startPdfScan(mode: PdfExportMode) = IosPdf.scan(mode)


/**
 * iOS の PDF 書き出し。入口は 2 つ。
 * - 書類スキャン: VisionKit の [VNDocumentCameraViewController]。複数ページを連続で読み取り、
 *   四隅の補正をスキャナ側で行う。
 * - 写真を選ぶ: PHPicker。
 * どちらも UIImage で縮小してから 1 つの PDF にまとめ、共有シートを出す。
 */
private object IosPdf {
    @Volatile
    var state: PdfExportState = PdfExportState.Idle

    private var pendingMode: PdfExportMode = PdfExportMode.STANDARD

    fun scan(mode: PdfExportMode) {
        pendingMode = mode
        state = PdfExportState.Working
        val controller = VNDocumentCameraViewController()
        controller.delegate = ScanDelegate()
        rootController()?.presentViewController(controller, animated = true, completion = null)
    }

    fun pick(mode: PdfExportMode) {
        pendingMode = mode
        state = PdfExportState.Working
        val configuration = PHPickerConfiguration().apply {
            selectionLimit = 12
            filter = PHPickerFilter.imagesFilter
        }
        val picker = PHPickerViewController(configuration = configuration)
        picker.delegate = PickerDelegate()
        rootController()?.presentViewController(picker, animated = true, completion = null)
    }

    fun receive(images: List<UIImage>) {
        if (images.isEmpty()) {
            state = PdfExportState.Cancelled
            return
        }
        val mode = pendingMode
        val settings = mode.settings()
        runCatching {
                var quality = settings.quality
                var encoded = images.mapNotNull { encode(it, quality, settings.maxDimension) }
                var pdf = PdfBuilder.build(encoded, PdfPageSize.A4)
                var attempt = 0
                while (pdf.size > mode.targetMb * 1024 * 1024 && mode.targetMb > 0 &&
                    attempt < 2 && quality > 35
                ) {
                    attempt += 1
                    quality = maxOf(35, quality - 12)
                    encoded = images.mapNotNull { encode(it, quality, settings.maxDimension) }
                    pdf = PdfBuilder.build(encoded, PdfPageSize.A4)
                }
                val path = NSTemporaryDirectory() + "openyaske.pdf"
                pdf.usePinned { pinned ->
                    NSData.create(bytes = pinned.addressOf(0), length = pdf.size.toULong())
                        .writeToFile(path, atomically = true)
                }
                state = PdfExportState.Done(pdf.size)
                dispatch_async(dispatch_get_main_queue()) {
                    share(path)
                }
        }.onFailure { error ->
            state = PdfExportState.Failed(error.message ?: "export failed")
        }
    }

    private data class Settings(val quality: Int, val maxDimension: Int)

    private fun PdfExportMode.settings(): Settings = when (this) {
        PdfExportMode.EXTREME -> Settings(40, 1000)
        PdfExportMode.LIGHT -> Settings(60, 1280)
        PdfExportMode.STANDARD -> Settings(78, 1600)
        PdfExportMode.HIGH -> Settings(90, 2048)
        PdfExportMode.GRAY -> Settings(70, 1400)
        PdfExportMode.ORIGINAL -> Settings(100, 4096)
    }

    private fun encode(image: UIImage, quality: Int, maxDimension: Int): PdfImage? {
        val width = image.size.useContents { width }
        val height = image.size.useContents { height }
        if (width <= 0.0 || height <= 0.0) return null
        val longest = maxOf(width, height)
        val scale = minOf(1.0, maxDimension / longest)
        val targetWidth = maxOf(1.0, width * scale)
        val targetHeight = maxOf(1.0, height * scale)
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(targetWidth, targetHeight), false, 1.0)
        image.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
        val resized = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        val jpeg = UIImageJPEGRepresentation(resized ?: image, quality / 100.0) ?: return null
        return PdfImage(jpeg.toByteArray(), targetWidth.toInt(), targetHeight.toInt())
    }

    private fun share(path: String) {
        val url = NSURL.fileURLWithPath(path)
        val controller = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
        val root = rootController() ?: return
        root.presentViewController(controller, animated = true, completion = null)
    }
}

private class PickerDelegate : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val providers = didFinishPicking.mapNotNull { (it as? PHPickerResult)?.itemProvider }
        if (providers.isEmpty()) {
            IosPdf.state = PdfExportState.Cancelled
            return
        }
        val collected = mutableListOf<UIImage>()
        var remaining = providers.size
        providers.forEach { provider ->
            provider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
                if (data != null) UIImage(data = data)?.let { collected.add(it) }
                remaining -= 1
                if (remaining == 0) {
                    IosPdf.receive(collected.toList())
                }
            }
        }
    }
}

/** VisionKit のスキャン結果を受け取る。 */
private class ScanDelegate : NSObject(), VNDocumentCameraViewControllerDelegateProtocol {
    override fun documentCameraViewController(
        controller: VNDocumentCameraViewController,
        didFinishWithScan: VNDocumentCameraScan
    ) {
        controller.dismissViewControllerAnimated(true, completion = null)
        val count = didFinishWithScan.pageCount.toInt()
        val pages = (0 until count).map { didFinishWithScan.imageOfPageAtIndex(it.toULong()) }
        IosPdf.receive(pages)
    }

    override fun documentCameraViewControllerDidCancel(controller: VNDocumentCameraViewController) {
        controller.dismissViewControllerAnimated(true, completion = null)
        IosPdf.state = PdfExportState.Cancelled
    }

    override fun documentCameraViewController(
        controller: VNDocumentCameraViewController,
        didFailWithError: NSError
    ) {
        controller.dismissViewControllerAnimated(true, completion = null)
        IosPdf.state = PdfExportState.Failed(didFailWithError.localizedDescription)
    }
}

private fun rootController(): UIViewController? =
    UIApplication.sharedApplication.keyWindow?.rootViewController

private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)
    val result = ByteArray(length)
    result.usePinned { pinned ->
        platform.posix.memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return result
}

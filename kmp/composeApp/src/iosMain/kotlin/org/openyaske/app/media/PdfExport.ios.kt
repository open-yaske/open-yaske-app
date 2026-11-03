@file:OptIn(ExperimentalForeignApi::class)

package org.openyaske.app.media

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
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
import platform.darwin.NSObject
import kotlin.concurrent.Volatile
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import org.openyaske.core.media.PdfBuilder
import org.openyaske.core.media.PdfImage
import org.openyaske.core.media.PdfPageSize

actual fun pdfExportSupported(): Boolean = true

actual fun pdfExportState(): PdfExportState = IosPdf.state

actual fun startPdfExport(mode: PdfExportMode) = IosPdf.start(mode)

actual fun pdfCaptureSupported(): Boolean = false

actual fun startPdfCapture(mode: PdfExportMode) = Unit

/** iOS の PDF 書き出し。PHPicker で選び、UIImage で縮小して共有する。 */
private object IosPdf {
    @Volatile
    var state: PdfExportState = PdfExportState.Idle

    private var pendingMode: PdfExportMode = PdfExportMode.STANDARD

    fun start(mode: PdfExportMode) {
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

    fun receive(images: List<PickedBytes>) {
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

    private fun encode(source: PickedBytes, quality: Int, maxDimension: Int): PdfImage? {
        val data = source.data
        val image = UIImage(data = data)
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

private class PickedBytes(val data: NSData)

private class PickerDelegate : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val providers = didFinishPicking.mapNotNull { (it as? PHPickerResult)?.itemProvider }
        if (providers.isEmpty()) {
            IosPdf.state = PdfExportState.Cancelled
            return
        }
        val collected = mutableListOf<PickedBytes>()
        var remaining = providers.size
        providers.forEach { provider ->
            provider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
                if (data != null) collected.add(PickedBytes(data))
                remaining -= 1
                if (remaining == 0) {
                    IosPdf.receive(collected.toList())
                }
            }
        }
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

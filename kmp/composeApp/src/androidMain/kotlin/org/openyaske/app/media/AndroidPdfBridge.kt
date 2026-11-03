package org.openyaske.app.media

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openyaske.core.media.PdfBuilder
import org.openyaske.core.media.PdfImage
import org.openyaske.core.media.PdfPageSize

/** Android の PDF 書き出し。Photo Picker の結果を受けて圧縮し、共有する。 */
object AndroidPdfBridge {
    private var activity: ComponentActivity? = null
    private var launcher: ActivityResultLauncher<String>? = null
    private var captureLauncher: ActivityResultLauncher<Void?>? = null

    @Volatile
    private var state: PdfExportState = PdfExportState.Idle

    fun attach(
        activity: ComponentActivity,
        launcher: ActivityResultLauncher<String>,
        capture: ActivityResultLauncher<Void?>
    ) {
        this.activity = activity
        this.launcher = launcher
        this.captureLauncher = capture
    }

    fun startCapture(mode: PdfExportMode) {
        pendingMode = mode
        state = PdfExportState.Working
        val capture = captureLauncher
        if (capture == null) {
            state = PdfExportState.Failed("camera is not ready")
            return
        }
        capture.launch(null)
    }

    fun onCaptured(bitmap: Bitmap?) {
        val context = activity ?: return
        if (bitmap == null) {
            state = PdfExportState.Cancelled
            return
        }
        val mode = pendingMode
        CoroutineScope(Dispatchers.IO).launch {
            state = PdfExportState.Working
            runCatching {
                val settings = mode.settings()
                val image = encodeBitmap(bitmap, settings)
                val pdf = PdfBuilder.build(listOf(image), PdfPageSize.A4)
                val file = File(context.cacheDir, "pdfs").apply { mkdirs() }.resolve("openyaske.pdf")
                file.writeBytes(pdf)
                state = PdfExportState.Done(pdf.size)
                withContext(Dispatchers.Main) { share(context, file) }
            }.onFailure { error ->
                state = PdfExportState.Failed(error.message ?: "export failed")
            }
        }
    }

    fun state(): PdfExportState = state

    fun start(mode: PdfExportMode) {
        state = PdfExportState.Working
        val launcher = launcher
        if (launcher == null) {
            state = PdfExportState.Failed("picker is not ready")
            return
        }
        launcher.launch("image/*")
        pendingMode = mode
    }

    private var pendingMode: PdfExportMode = PdfExportMode.STANDARD

    fun onResult(uris: List<Uri>) {
        val context = activity ?: return
        val mode = pendingMode
        if (uris.isEmpty()) {
            state = PdfExportState.Cancelled
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            state = PdfExportState.Working
            runCatching {
                val settings = mode.settings()
                var quality = settings.quality
                var images = encode(context, uris, settings.copy(quality = quality))
                var pdf = PdfBuilder.build(images, PdfPageSize.A4)
                var attempt = 0
                while (pdf.size > mode.targetMb * 1024 * 1024 && attempt < 2 && quality > 35) {
                    attempt += 1
                    quality = max(35, quality - 12)
                    images = encode(context, uris, settings.copy(quality = quality))
                    pdf = PdfBuilder.build(images, PdfPageSize.A4)
                }
                val file = File(context.cacheDir, "pdfs").apply { mkdirs() }
                    .resolve("openyaske.pdf")
                file.writeBytes(pdf)
                state = PdfExportState.Done(pdf.size)
                withContext(Dispatchers.Main) { share(context, file) }
            }.onFailure { error ->
                state = PdfExportState.Failed(error.message ?: "export failed")
            }
        }
    }

    private data class Settings(
        val quality: Int,
        val maxDimension: Int,
        val gray: Boolean,
        val binary: Boolean
    )

    private fun PdfExportMode.settings(): Settings = when (this) {
        PdfExportMode.EXTREME -> Settings(40, 1000, gray = true, binary = true)
        PdfExportMode.LIGHT -> Settings(60, 1280, gray = false, binary = false)
        PdfExportMode.STANDARD -> Settings(78, 1600, gray = false, binary = false)
        PdfExportMode.HIGH -> Settings(90, 2048, gray = false, binary = false)
        PdfExportMode.GRAY -> Settings(70, 1400, gray = true, binary = false)
        PdfExportMode.ORIGINAL -> Settings(100, 4096, gray = false, binary = false)
    }

    private fun encode(context: Context, uris: List<Uri>, settings: Settings): List<PdfImage> =
        uris.take(12).mapNotNull { uri ->
            runCatching { encodeOne(context, uri, settings) }.getOrNull()
        }

    private fun encodeBitmap(bitmap: Bitmap, settings: Settings): PdfImage {
        val longest = max(bitmap.width, bitmap.height)
        val scale = min(1f, settings.maxDimension.toFloat() / longest)
        val width = max(1, (bitmap.width * scale).toInt())
        val height = max(1, (bitmap.height * scale).toInt())
        val scaled = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, width, height, true) else bitmap
        if (settings.gray || settings.binary) {
            val pixels = IntArray(width * height)
            scaled.getPixels(pixels, 0, width, 0, 0, width, height)
            for (index in pixels.indices) {
                val color = pixels[index]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                var value = (r * 299 + g * 587 + b * 114) / 1000
                if (settings.binary) value = if (value > 150) 255 else 0
                pixels[index] = (0xFF shl 24) or (value shl 16) or (value shl 8) or value
            }
            scaled.setPixels(pixels, 0, width, 0, 0, width, height)
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, settings.quality, out)
        if (scaled !== bitmap) scaled.recycle()
        return PdfImage(out.toByteArray(), width, height)
    }

    private fun encodeOne(context: Context, uri: Uri, settings: Settings): PdfImage? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= settings.maxDimension) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val longest = max(decoded.width, decoded.height)
        val scale = min(1f, settings.maxDimension.toFloat() / longest)
        val width = max(1, (decoded.width * scale).toInt())
        val height = max(1, (decoded.height * scale).toInt())
        val scaled = if (scale < 1f) Bitmap.createScaledBitmap(decoded, width, height, true) else decoded

        if (settings.gray || settings.binary) {
            val pixels = IntArray(width * height)
            scaled.getPixels(pixels, 0, width, 0, 0, width, height)
            for (index in pixels.indices) {
                val color = pixels[index]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                var value = ((r * 299 + g * 587 + b * 114) / 1000)
                if (settings.binary) value = if (value > 150) 255 else 0
                pixels[index] = (0xFF shl 24) or (value shl 16) or (value shl 8) or value
            }
            scaled.setPixels(pixels, 0, width, 0, 0, width, height)
        }

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, settings.quality, out)
        if (scaled !== decoded) decoded.recycle()
        scaled.recycle()
        return PdfImage(out.toByteArray(), width, height)
    }

    private fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "PDF"))
    }
}

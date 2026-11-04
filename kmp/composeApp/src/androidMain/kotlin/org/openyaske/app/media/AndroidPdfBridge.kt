package org.openyaske.app.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import org.openyaske.core.media.PdfBuilder
import org.openyaske.core.media.PdfImage
import org.openyaske.core.media.PdfPageSize

/**
 * Android の PDF 書き出し。
 *
 * 2 つの入口を持つ。
 * - 書類スキャン: ML Kit Document Scanner（Google Play services）。複数ページを連続で読み取り、
 *   四隅の補正とフィルタをスキャナ側で行う。カメラを自前で起動するのではない。
 * - 写真を選ぶ: Photo Picker。アルバムの画像をそのままページにする。
 */
object AndroidPdfBridge {
    /** 一度に扱う枚数。スキャナと Picker で揃える。 */
    const val MAX_PAGES = 24

    private var activity: ComponentActivity? = null
    private var launcher: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var scanLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

    @Volatile
    private var state: PdfExportState = PdfExportState.Idle

    private var pendingMode: PdfExportMode = PdfExportMode.STANDARD

    fun attach(
        activity: ComponentActivity,
        launcher: ActivityResultLauncher<PickVisualMediaRequest>,
        scanner: ActivityResultLauncher<IntentSenderRequest>
    ) {
        this.activity = activity
        this.launcher = launcher
        this.scanLauncher = scanner
    }

    fun state(): PdfExportState = state

    /** 端末の書類スキャナが使えるか（Google Play services が必要）。 */
    fun scanSupported(): Boolean {
        val activity = activity ?: return false
        return GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(activity) == ConnectionResult.SUCCESS
    }

    /** 書類スキャナを開く。読み取り結果は [onScanResult] に返る。 */
    fun startScan(mode: PdfExportMode) {
        val activity = activity ?: return
        val scanner = scanLauncher
        if (scanner == null) {
            state = PdfExportState.Failed("scanner is not ready")
            return
        }
        pendingMode = mode
        state = PdfExportState.Working
        val options = GmsDocumentScannerOptions.Builder()
            // アルバムからの取り込みも許す（紙を撮り直さずに済む）。
            .setGalleryImportAllowed(true)
            .setPageLimit(MAX_PAGES)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            // FULL は四隅の補正・フィルタ・回転をユーザーが直せる。
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                runCatching {
                    scanner.launch(IntentSenderRequest.Builder(intentSender).build())
                }.onFailure {
                    state = PdfExportState.Failed(it.message ?: "scanner failed")
                }
            }
            .addOnFailureListener { error ->
                state = PdfExportState.Failed(error.message ?: "scanner failed")
            }
    }

    /** 書類スキャナの結果。取り消しは Cancelled として扱う。 */
    fun onScanResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            state = PdfExportState.Cancelled
            return
        }
        val result = GmsDocumentScanningResult.fromActivityResultIntent(data)
        val uris = result?.pages?.mapNotNull { it.imageUri } ?: emptyList()
        if (uris.isEmpty()) {
            state = PdfExportState.Cancelled
            return
        }
        build(pendingMode, uris)
    }

    fun start(mode: PdfExportMode) {
        state = PdfExportState.Working
        val launcher = launcher
        if (launcher == null) {
            state = PdfExportState.Failed("picker is not ready")
            return
        }
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
        pendingMode = mode
    }


    fun onResult(uris: List<Uri>) {
        if (uris.isEmpty()) {
            state = PdfExportState.Cancelled
            return
        }
        build(pendingMode, uris)
    }

    private fun build(mode: PdfExportMode, uris: List<Uri>) {
        val context = activity ?: return
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
        uris.take(MAX_PAGES).mapNotNull { uri ->
            runCatching { encodeOne(context, uri, settings) }.getOrNull()
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

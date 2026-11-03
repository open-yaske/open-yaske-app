package org.openyaske.app.ui.timetable

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Android は filesDir に JPEG を置く。選択は Photo Picker（GetContent）で受ける。
 * 画像は長辺 1600px・JPEG 80% に縮小してから保存する。
 */
object AndroidTimetableBackground {
    const val FILE_NAME = "timetable-background.jpg"

    /** MainActivity が登録したピッカー。結果はここへ返す。 */
    var pick: (suspend () -> Uri?)? = null

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    fun read(context: Context): ByteArray? =
        runCatching { file(context).takeIf { it.exists() }?.readBytes() }.getOrNull()

    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }

    fun saveJpeg(context: Context, uri: Uri): Boolean = runCatching {
        val resolver = context.contentResolver
        val original = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: return@runCatching false
        val longest = max(original.width, original.height)
        val scale = min(1f, 1600f / longest)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt().coerceAtLeast(1),
                (original.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            original
        }
        ByteArrayOutputStream().use { output ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, output)
            file(context).writeBytes(output.toByteArray())
        }
        if (scaled !== original) scaled.recycle()
        original.recycle()
        true
    }.getOrDefault(false)
}

private var appContext: Context? = null

/** MainActivity から呼ぶ。 */
fun attachTimetableBackground(context: Context) {
    appContext = context.applicationContext
}

actual object TimetableBackground {
    actual suspend fun load(): ByteArray? = appContext?.let { AndroidTimetableBackground.read(it) }

    actual suspend fun pickAndSave(): Boolean {
        val context = appContext ?: return false
        val uri = AndroidTimetableBackground.pick?.invoke() ?: return false
        return AndroidTimetableBackground.saveJpeg(context, uri)
    }

    actual suspend fun clear() {
        appContext?.let { AndroidTimetableBackground.clear(it) }
    }
}

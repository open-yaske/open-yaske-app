package org.openyaske.app.ui.timetable

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.memcpy

/**
 * iOS は Documents に JPEG を置く。画像の選択 UI は今回の範囲外（保存・読み出し・削除のみ）。
 */
@OptIn(ExperimentalForeignApi::class)
actual object TimetableBackground {
    private const val FILE_NAME = "timetable-background.jpg"

    private fun path(): String {
        val documents = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).first() as String
        return "$documents/$FILE_NAME"
    }

    actual suspend fun load(): ByteArray? {
        val data = NSData.dataWithContentsOfFile(path()) ?: return null
        val length = data.length.toInt()
        if (length == 0) return null
        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
        return bytes
    }

    actual suspend fun pickAndSave(): Boolean = false

    actual suspend fun clear() {
        NSFileManager.defaultManager.removeItemAtPath(path(), null)
    }
}

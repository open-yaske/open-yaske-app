@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.openyaske.app.ui.timetable

import kotlinx.coroutines.delay
import org.openyaske.core.media.decodeBase64

private fun startLoad(): Unit = js("window.AppMedia.bgLoad()")
private fun startPick(): Unit = js("window.AppMedia.bgPick()")
private fun clearStore(): Unit = js("window.AppMedia.bgClear()")
private fun status(): String = js("window.AppMedia.bgStatus()")
private fun dataUrl(): String = js("window.AppMedia.bgDataUrl()")

actual object TimetableBackground {
    actual suspend fun load(): ByteArray? {
        startLoad()
        waitUntilReady()
        return decode(dataUrl())
    }

    actual suspend fun pickAndSave(): Boolean {
        val before = dataUrl()
        startPick()
        waitUntilReady()
        val after = dataUrl()
        return after.isNotBlank() && after != before || (after.isNotBlank() && before.isBlank())
    }

    actual suspend fun clear() {
        clearStore()
        waitUntilReady()
    }

    private suspend fun waitUntilReady() {
        var tries = 0
        while (status() != "ready" && tries < 600) {
            delay(100)
            tries += 1
        }
    }

    private fun decode(url: String): ByteArray? {
        if (url.isBlank()) return null
        val comma = url.indexOf(',')
        if (comma < 0) return null
        return runCatching { decodeBase64(url.substring(comma + 1)) }.getOrNull()
    }
}

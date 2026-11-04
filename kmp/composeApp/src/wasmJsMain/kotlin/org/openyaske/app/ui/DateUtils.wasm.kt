@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.openyaske.app.ui

actual fun nowMinutes(): Int = js("(() => { const d = new Date(); return d.getHours() * 60 + d.getMinutes(); })()")

actual fun todayIso(): String = js(
    "(() => { const d = new Date(); const p = (n) => String(n).padStart(2, '0');" +
        " return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()); })()"
)

private fun jsDateNow(): Double = js("Date.now()")

actual fun nowMillis(): Long = jsDateNow().toLong()

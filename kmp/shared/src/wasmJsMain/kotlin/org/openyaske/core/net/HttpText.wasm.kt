@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.openyaske.core.net

import kotlinx.coroutines.delay

private fun start(url: String, key: String): Unit = js(
    "(() => { window.__net = window.__net || {}; window.__net[key] = { s: 'loading' }; fetch(url).then(r => r.text()).then(t => { window.__net[key] = { s: 'done', t: t }; }).catch(() => { window.__net[key] = { s: 'error', t: '' }; }); })()"
)

private fun status(key: String): String = js(
    "(window.__net && window.__net[key] && window.__net[key].s) || 'idle'"
)

private fun text(key: String): String = js(
    "(window.__net && window.__net[key] && window.__net[key].t) || ''"
)

actual suspend fun httpGetText(url: String): String? {
    val key = "k" + url.hashCode().toString().replace("-", "m")
    start(url, key)
    var tries = 0
    while (tries < 100) {
        when (status(key)) {
            "done" -> return text(key)
            "error" -> return null
        }
        delay(100)
        tries += 1
    }
    return null
}

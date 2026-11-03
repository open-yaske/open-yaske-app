@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.openyaske.app.ui

import kotlinx.browser.window

actual fun initialWebPath(): String? =
    js("(() => { const h = globalThis.location.hash; return h && h.indexOf('#/') === 0 ? h.slice(1) : globalThis.location.pathname; })()")

actual fun initialWebQuery(): String? =
    js("globalThis.location.search")

actual fun pushWebPath(path: String): Unit =
    js("globalThis.history.pushState(null, '', '#' + path)")

actual fun replaceWebPath(path: String): Unit =
    js("globalThis.history.replaceState(null, '', '#' + path)")

actual fun observeWebBack(onBack: () -> Unit) {
    window.addEventListener("popstate", { _ -> onBack() })
}

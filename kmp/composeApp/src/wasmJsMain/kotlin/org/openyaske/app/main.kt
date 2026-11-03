package org.openyaske.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.openyaske.core.api.createWasmCoreApi
import org.w3c.dom.HTMLElement

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val api = createWasmCoreApi()
    val root = document.getElementById("compose-root") as HTMLElement
    ComposeViewport(root) {
        App(api)
    }
}

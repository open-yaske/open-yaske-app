package org.openyaske.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import org.openyaske.app.ui.strings.EN
import org.openyaske.app.ui.strings.JA
import org.openyaske.app.ui.strings.KO
import org.openyaske.app.ui.strings.ZH

/** 画面で使う文言。言語設定の変更を即時に反映する。 */
class UiStrings(private val values: Map<String, String>) {
    operator fun get(key: String): String = values[key] ?: key
}

val LocalUiStrings = staticCompositionLocalOf { uiStrings("ja") }

fun uiStrings(locale: String): UiStrings = when (locale.take(2)) {
    "en" -> UiStrings(EN)
    "zh" -> UiStrings(ZH)
    "ko" -> UiStrings(KO)
    else -> UiStrings(JA)
}

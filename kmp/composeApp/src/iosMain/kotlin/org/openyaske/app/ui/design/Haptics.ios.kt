package org.openyaske.app.ui.design

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

/**
 * iOS は軽いインパクトを 1 つだけ使い回す。毎回作ると準備に時間がかかる。
 */
private val lightGenerator: UIImpactFeedbackGenerator by lazy {
    UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleLight).also {
        it.prepare()
    }
}

actual fun tapFeedback() {
    lightGenerator.impactOccurred()
}

actual fun selectFeedback() {
    lightGenerator.prepare()
    lightGenerator.impactOccurred()
}

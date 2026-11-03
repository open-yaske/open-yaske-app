package org.openyaske.app.ui.design

import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Android は View の触覚フィードバックを使う。
 * `VIBRATE` 権限は不要。
 */
object AndroidHaptics {
    var view: View? = null
}

actual fun tapFeedback() {
    AndroidHaptics.view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
}

actual fun selectFeedback() {
    AndroidHaptics.view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

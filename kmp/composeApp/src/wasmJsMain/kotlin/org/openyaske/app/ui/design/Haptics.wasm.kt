package org.openyaske.app.ui.design

/**
 * Web では何もしない。
 * `navigator.vibrate` は Android Chrome 以外で意味がなく、例外も投げたくないので触らない。
 */
actual fun tapFeedback() = Unit

actual fun selectFeedback() = Unit

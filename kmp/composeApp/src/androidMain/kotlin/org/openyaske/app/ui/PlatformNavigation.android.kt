package org.openyaske.app.ui

actual fun initialWebPath(): String? = null

actual fun initialWebQuery(): String? = null

actual fun pushWebPath(path: String) = Unit

actual fun replaceWebPath(path: String) = Unit

actual fun observeWebBack(onBack: () -> Unit) = Unit

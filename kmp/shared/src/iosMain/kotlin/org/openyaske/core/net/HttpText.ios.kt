package org.openyaske.core.net

/**
 * iOS は今回の範囲外。天気・乗換カードは null を受けて非表示になる。
 * 実装するときは NSURLSession の dataTask を suspendCancellableCoroutine で包む。
 */
actual suspend fun httpGetText(url: String): String? = null

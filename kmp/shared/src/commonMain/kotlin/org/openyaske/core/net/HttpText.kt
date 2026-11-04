package org.openyaske.core.net

/**
 * 文字列を 1 つ取ってくるだけの最小の HTTP。
 *
 * 返り値は本文。失敗（非 2xx・通信不可）は null。
 * 天気と乗換案内だけが使う。実行時にサーバは持たない。
 */
expect suspend fun httpGetText(url: String): String?

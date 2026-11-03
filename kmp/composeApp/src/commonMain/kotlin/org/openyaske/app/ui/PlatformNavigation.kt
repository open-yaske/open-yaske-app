package org.openyaske.app.ui

import androidx.compose.runtime.Composable

/**
 * 画面遷移の戻る操作。
 *
 * Android はシステム Back、Web はブラウザ Back を共通の BackHandler で受ける。
 * iOS はジェスチャー/戻るボタンを画面側で扱う。
 */
/**
 * 起動時のパス。Web は `#/timetable` のようなハッシュを返し、
 * ハッシュが無ければ pathname を返す。Android / iOS は null。
 */
expect fun initialWebPath(): String?

/**
 * 起動時のクエリ（`?theme=dark` など）。ハッシュより前に読む。
 * Web 以外は null。
 */
expect fun initialWebQuery(): String?

expect fun pushWebPath(path: String)

expect fun replaceWebPath(path: String)

/**
 * Web のブラウザ戻る（popstate）を購読する。
 * Android と iOS はシステムの戻るを BackHandler が受けるので何もしない。
 */
expect fun observeWebBack(onBack: () -> Unit)

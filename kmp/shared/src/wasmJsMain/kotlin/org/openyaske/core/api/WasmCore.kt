@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.openyaske.core.api

import org.openyaske.core.data.InMemoryRepository
import org.openyaske.core.model.UserData
import org.openyaske.core.validation.UserDataCodec
import kotlin.js.js

private const val DATA_KEY = "openyaske.core.data.v2"
private const val SETTINGS_KEY = "openyaske.core.settings.v1"

private fun readLocalStorage(key: String): String? =
    js("globalThis.localStorage.getItem(key)")

private fun writeLocalStorage(key: String, value: String): Unit =
    js("globalThis.localStorage.setItem(key, value)")

/**
 * Wasm 用の CoreApi。
 *
 * SQLDelight の web worker driver は非同期のため、まずは localStorage に
 * JSON を保存するインメモリ実装を使う。ユーザーデータは数百 KB 程度なので
 * localStorage の容量内に収まる。
 */
fun createWasmCoreApi(): CoreApi {
    val stored = readLocalStorage(DATA_KEY)
    val initial = stored?.let { UserDataCodec.decode(it).data } ?: UserData()
    val repository = InMemoryRepository(
        initial = initial,
        settingsJson = readLocalStorage(SETTINGS_KEY),
        onChange = { data -> writeLocalStorage(DATA_KEY, UserDataCodec.encode(data)) },
        onSettingsChange = { json -> writeLocalStorage(SETTINGS_KEY, json) }
    )
    return CoreApi(repository)
}

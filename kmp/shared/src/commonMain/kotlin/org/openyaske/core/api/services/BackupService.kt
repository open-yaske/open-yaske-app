package org.openyaske.core.api.services

import org.openyaske.core.model.UserData
import org.openyaske.core.api.CoreException
import org.openyaske.core.validation.UserDataCodec
import org.openyaske.core.validation.NormalizeReport

class BackupService internal constructor(private val store: CoreStore) {

    fun exportJson(): String = UserDataCodec.encode(store.snapshot)

    /** すべての学習データを空にする。設定は CoreApi 側で既定に戻す。 */
    fun clearAll() {
        store.repository.replaceAll(UserData())
        store.notifyChanged()
    }

    fun importJson(raw: String): NormalizeReport {
        val normalized = UserDataCodec.decode(raw)
        store.repository.replaceAll(normalized.data)
        store.notifyChanged()
        return normalized.report
    }
}

package org.openyaske.core.api.services

import org.openyaske.core.api.CoreException
import org.openyaske.core.api.AppSettings

class SettingsService internal constructor(private val store: CoreStore) {

    fun updateSettings(transform: (AppSettings) -> AppSettings): AppSettings {
        val next = transform(store.settings.value).normalized()
        store.repository.saveSettingsJson(
            store.json.encodeToString(AppSettings.serializer(), next),
            AppSettings.CURRENT_SCHEMA_VERSION,
            store.clock()
        )
        store.settings.value = next
        return next
    }
}

package org.openyaske.core.api.services

import org.openyaske.core.api.CoreException
import org.openyaske.core.model.SyllabusEntry
import org.openyaske.core.api.SyllabusCatalog
import org.openyaske.core.api.SyllabusSearchHit

class SyllabusService internal constructor(private val store: CoreStore) {

    fun setSyllabus(entries: List<SyllabusEntry>) {
        store.catalog.replace(entries)
        store.syllabusCount.value = store.catalog.size
    }

    fun searchSyllabus(
        query: String,
        year: Int? = null,
        weekday: org.openyaske.core.model.Weekday? = null,
        period: Int? = null,
        programType: String? = null,
        limit: Int = 30
    ): List<SyllabusSearchHit> =
        store.catalog.search(query, year, weekday, period, programType, limit)
}

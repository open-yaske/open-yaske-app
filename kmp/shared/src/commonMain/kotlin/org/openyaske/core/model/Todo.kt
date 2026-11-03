package org.openyaske.core.model

import kotlinx.serialization.Serializable

/** TODO のサブタスク。APK 版は JSON 文字列 1 列に潰していた。 */
@Serializable
data class SubTask(
    val id: String,
    val title: String,
    val done: Boolean = false
)

/** 繰り返し規則。BIWEEKLY は APK 版にあって Svelte 版になかった。 */
@Serializable
data class RepeatRule(
    val pattern: RepeatPattern = RepeatPattern.NONE,
    val count: Int? = null,
    val untilMillis: Long? = null
) {
    val isRepeating: Boolean get() = pattern != RepeatPattern.NONE
}

/** TODO リスト（Inbox などのシステムリストを含む）。 */
@Serializable
data class TodoList(
    val id: String,
    val name: String,
    val systemKey: String? = null,
    val sortOrder: Int = 0,
    val createdAtMillis: Long = 0L
)

@Serializable
data class Todo(
    val id: String,
    val title: String,
    val done: Boolean = false,
    val courseId: String? = null,
    val listId: String? = null,
    val category: TodoCategory? = null,
    val details: String? = null,
    val dueAtMillis: Long? = null,
    /** 締切の何分前に通知するか。0 は同時刻、null は通知なし。 */
    val notifyMinutesBefore: Int? = null,
    val priority: TodoPriority? = null,
    val repeat: RepeatRule? = null,
    val color: String? = null,
    val subtasks: List<SubTask> = emptyList(),
    val parentTodoId: String? = null,
    val trashedAtMillis: Long? = null,
    val sortOrder: Int = 0,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    val isTrashed: Boolean get() = trashedAtMillis != null
    val isSubtask: Boolean get() = parentTodoId != null

    /** 締切と通知設定から実際の通知時刻を返す。 */
    fun notificationAtMillis(): Long? {
        val due = dueAtMillis ?: return null
        val offset = notifyMinutesBefore?.coerceAtLeast(0) ?: return null
        return due - offset * 60_000L
    }
}

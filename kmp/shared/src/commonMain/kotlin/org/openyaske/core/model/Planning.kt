package org.openyaske.core.model

import kotlinx.serialization.Serializable

/** 学期の種類。UEC の前学期 / 後期に加え、通年・集中・クォーターを表現する。 */
@Serializable
enum class TermKind {
    @kotlinx.serialization.SerialName("semester")
    SEMESTER,

    @kotlinx.serialization.SerialName("full_year")
    FULL_YEAR,

    @kotlinx.serialization.SerialName("intensive")
    INTENSIVE,

    @kotlinx.serialization.SerialName("quarter")
    QUARTER,

    @kotlinx.serialization.SerialName("other")
    OTHER
}

/**
 * 学期。ID は `2026-spring` のように年と学期から導出する。
 * 期間は ISO 日付で持ち、授業回の生成に使う。
 */
@Serializable
data class Term(
    val id: String,
    val year: Int,
    val kind: TermKind = TermKind.SEMESTER,
    val season: Semester? = null,
    val startDate: String,
    val endDate: String,
    val label: String = ""
) {
    companion object {
        fun idOf(year: Int, season: Semester): String = "$year-${season.name.lowercase()}"

        fun semester(year: Int, season: Semester, startDate: String, endDate: String): Term =
            Term(
                id = idOf(year, season),
                year = year,
                kind = TermKind.SEMESTER,
                season = season,
                startDate = startDate,
                endDate = endDate
            )
    }
}

/** 科目の形態。通常の時間割、時間割外、集中講義、連携大学、ユーザー定義。 */
@Serializable
enum class CourseKind {
    @kotlinx.serialization.SerialName("regular")
    REGULAR,

    @kotlinx.serialization.SerialName("extra")
    EXTRA,

    @kotlinx.serialization.SerialName("intensive")
    INTENSIVE,

    @kotlinx.serialization.SerialName("partner")
    PARTNER,

    @kotlinx.serialization.SerialName("user")
    USER
}

/** 科目情報の出所。 */
@Serializable
enum class CourseSource {
    @kotlinx.serialization.SerialName("public_syllabus")
    PUBLIC_SYLLABUS,

    @kotlinx.serialization.SerialName("user")
    USER,

    @kotlinx.serialization.SerialName("partner")
    PARTNER
}

/** 単位系。連携大学の ECTS などをそのまま保持できるようにする。 */
@Serializable
enum class CreditSystem {
    @kotlinx.serialization.SerialName("uec")
    UEC,

    @kotlinx.serialization.SerialName("ects")
    ECTS,

    @kotlinx.serialization.SerialName("other")
    OTHER
}

/** 授業回の種類。 */
@Serializable
enum class SessionKind {
    @kotlinx.serialization.SerialName("regular")
    REGULAR,

    @kotlinx.serialization.SerialName("intensive")
    INTENSIVE,

    @kotlinx.serialization.SerialName("makeup")
    MAKEUP,

    @kotlinx.serialization.SerialName("exam")
    EXAM,

    @kotlinx.serialization.SerialName("extra")
    EXTRA,

    @kotlinx.serialization.SerialName("partner")
    PARTNER
}

/** 授業回の状態。 */
@Serializable
enum class SessionStatus {
    @kotlinx.serialization.SerialName("scheduled")
    SCHEDULED,

    @kotlinx.serialization.SerialName("cancelled")
    CANCELLED,

    @kotlinx.serialization.SerialName("moved")
    MOVED
}

/**
 * 実際の授業回。
 *
 * 通常科目は学期の開始日から週次パターンを展開して生成する。
 * 集中講義・連携大学・補講は日付と時刻を直接持つ。
 * 出席はこの ID に紐づけるので、補講や振替でも同じ仕組みで扱える。
 */
@Serializable
data class CourseSession(
    val id: String,
    val courseId: String,
    /** YYYY-MM-DD */
    val date: String,
    /** 時限で表せる場合の開始時限。時刻指定の講義では null。 */
    val startPeriod: Int? = null,
    val periodCount: Int = 1,
    /** HH:MM。時限を使わない集中講義・連携大学向け。 */
    val startTime: String? = null,
    val endTime: String? = null,
    val room: String? = null,
    val kind: SessionKind = SessionKind.REGULAR,
    val status: SessionStatus = SessionStatus.SCHEDULED,
    val note: String? = null,
    /** 振替元の授業回 ID。補講を元の回と対応付ける。 */
    val replacesSessionId: String? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    /** 端末同期の墓標。null なら有効。 */
    val deletedAtMillis: Long? = null
)

/** 休日の規則。`periods` が null なら終日休み、指定があればその時限だけ休み。 */
@Serializable
data class HolidayRule(
    val date: String,
    val periods: List<Int>? = null,
    val name: String = ""
)

/** 週次パターンから生成した授業回に対する上書き。 */
@Serializable
data class SessionOverride(
    val date: String,
    val status: SessionStatus,
    val note: String? = null
)

/** 学校行事・学事予定。 */
@Serializable
enum class SchoolEventKind {
    @kotlinx.serialization.SerialName("academic")
    ACADEMIC,

    @kotlinx.serialization.SerialName("holiday")
    HOLIDAY,

    @kotlinx.serialization.SerialName("exam")
    EXAM,

    @kotlinx.serialization.SerialName("makeup")
    MAKEUP,

    @kotlinx.serialization.SerialName("cancelled_class")
    CANCELLED_CLASS,

    @kotlinx.serialization.SerialName("other")
    OTHER
}

@Serializable
enum class SchoolEventSource {
    @kotlinx.serialization.SerialName("public")
    PUBLIC,

    @kotlinx.serialization.SerialName("user")
    USER,

    @kotlinx.serialization.SerialName("derived")
    DERIVED
}

@Serializable
data class SchoolEvent(
    val id: String,
    val title: String,
    val kind: SchoolEventKind = SchoolEventKind.ACADEMIC,
    /** YYYY-MM-DD */
    val date: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val allDay: Boolean = true,
    /** 場所。空でもよい。 */
    val location: String? = null,
    val courseId: String? = null,
    val sessionId: String? = null,
    val note: String? = null,
    val source: SchoolEventSource = SchoolEventSource.PUBLIC,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    /** 端末同期の墓標。null なら有効。 */
    val deletedAtMillis: Long? = null
)

/** 科目に紐づく課題。 */
@Serializable
enum class TaskKind {
    @kotlinx.serialization.SerialName("assignment")
    ASSIGNMENT,

    @kotlinx.serialization.SerialName("report")
    REPORT,

    @kotlinx.serialization.SerialName("quiz")
    QUIZ,

    @kotlinx.serialization.SerialName("exam")
    EXAM,

    @kotlinx.serialization.SerialName("prep")
    PREP,

    @kotlinx.serialization.SerialName("other")
    OTHER
}

@Serializable
enum class TaskStatus {
    @kotlinx.serialization.SerialName("open")
    OPEN,

    @kotlinx.serialization.SerialName("done")
    DONE,

    @kotlinx.serialization.SerialName("submitted")
    SUBMITTED,

    @kotlinx.serialization.SerialName("cancelled")
    CANCELLED
}

@Serializable
data class CourseTask(
    val id: String,
    /** 科目に紐づかない TODO は null。 */
    val courseId: String? = null,
    val sessionId: String? = null,
    val kind: TaskKind = TaskKind.ASSIGNMENT,
    val title: String,
    /** YYYY-MM-DD */
    val dueDate: String? = null,
    /** HH:MM */
    val dueTime: String? = null,
    val status: TaskStatus = TaskStatus.OPEN,
    val priority: TodoPriority? = null,
    /** 繰り返し。null は繰り返さない。完了にしたとき次の 1 件を作る。 */
    val repeat: RepeatPattern? = null,
    val notes: String? = null,
    val url: String? = null,
    val deletedAtMillis: Long? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    val isOpen: Boolean get() = status == TaskStatus.OPEN
    val isDeleted: Boolean get() = deletedAtMillis != null
}

/** 成績の状態。未確定・合否・除外・認定を GPA と分けて持つ。 */
@Serializable
enum class GradeState {
    @kotlinx.serialization.SerialName("not_graded")
    NOT_GRADED,

    @kotlinx.serialization.SerialName("graded")
    GRADED,

    @kotlinx.serialization.SerialName("pass_fail")
    PASS_FAIL,

    @kotlinx.serialization.SerialName("excluded")
    EXCLUDED,

    @kotlinx.serialization.SerialName("transfer")
    TRANSFER,

    @kotlinx.serialization.SerialName("in_progress")
    IN_PROGRESS
}

@Serializable
data class CourseGrade(
    val courseId: String,
    val state: GradeState = GradeState.NOT_GRADED,
    val grade: GpaGrade? = null,
    /** 認定単位数が科目と異なる場合だけ持つ。 */
    val creditsEarned: Int? = null,
    val countsTowardGpa: Boolean = true,
    val note: String? = null,
    val updatedAtMillis: Long = 0L
)

/** 「この科目が S だったら」を試すための上書き。 */
@Serializable
data class GpaScenario(
    val id: String,
    val name: String,
    val overrides: Map<String, GpaGrade> = emptyMap(),
    val createdAtMillis: Long = 0L
)

/** 端末のカレンダー / リマインダーとの対応付け。 */
@Serializable
enum class SyncProvider {
    @kotlinx.serialization.SerialName("calendar")
    CALENDAR,

    @kotlinx.serialization.SerialName("reminders")
    REMINDERS
}

@Serializable
enum class SyncLocalType {
    @kotlinx.serialization.SerialName("session")
    SESSION,

    @kotlinx.serialization.SerialName("event")
    EVENT,

    @kotlinx.serialization.SerialName("task")
    TASK
}

@Serializable
enum class SyncDirection {
    @kotlinx.serialization.SerialName("to_device")
    TO_DEVICE,

    @kotlinx.serialization.SerialName("from_device")
    FROM_DEVICE,

    @kotlinx.serialization.SerialName("both")
    BOTH
}

@Serializable
data class ExternalSyncLink(
    val id: String,
    val localType: SyncLocalType,
    val localId: String,
    val provider: SyncProvider,
    val externalId: String,
    /** 専用カレンダー / リマインダーリストの ID。 */
    val containerId: String? = null,
    val direction: SyncDirection = SyncDirection.TO_DEVICE,
    val lastSyncedAtMillis: Long = 0L,
    val deletedAtMillis: Long? = null
)

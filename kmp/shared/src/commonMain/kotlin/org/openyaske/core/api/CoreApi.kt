package org.openyaske.core.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import org.openyaske.core.api.services.AttendanceService
import org.openyaske.core.api.services.BackupService
import org.openyaske.core.api.services.CourseService
import org.openyaske.core.api.services.CoreStore
import org.openyaske.core.api.services.GradeService
import org.openyaske.core.api.services.ScheduleService
import org.openyaske.core.api.services.SettingsService
import org.openyaske.core.api.services.SyllabusService
import org.openyaske.core.api.services.TaskService
import org.openyaske.core.data.CoreRepository
import org.openyaske.core.logic.AgendaEntry
import org.openyaske.core.logic.AgendaProjector
import org.openyaske.core.logic.AttendanceCalculator
import org.openyaske.core.logic.AttendanceSummary
import org.openyaske.core.logic.GpaCalculator
import org.openyaske.core.logic.GpaSummary
import org.openyaske.core.logic.PeriodTimes
import org.openyaske.core.logic.TimetableIssue
import org.openyaske.core.logic.TimetableValidator
import org.openyaske.core.model.AttendanceRecord
import org.openyaske.core.model.AttendanceStatus
import org.openyaske.core.model.Course
import org.openyaske.core.model.CourseGrade
import org.openyaske.core.model.CourseSession
import org.openyaske.core.model.CourseTask
import org.openyaske.core.model.GpaGrade
import org.openyaske.core.model.GpaScenario
import org.openyaske.core.model.GradeState
import org.openyaske.core.model.SchoolEvent
import org.openyaske.core.model.SessionKind
import org.openyaske.core.model.SessionStatus
import org.openyaske.core.model.SyllabusEntry
import org.openyaske.core.model.TaskStatus
import org.openyaske.core.model.Term
import org.openyaske.core.model.TimetableSlot
import org.openyaske.core.model.UserData
import org.openyaske.core.model.Weekday
import org.openyaske.core.validation.IsoDate
import org.openyaske.core.validation.NormalizeReport
import org.openyaske.core.validation.UserDataCodec
import kotlin.random.Random

class CoreException(message: String) : IllegalArgumentException(message)

fun interface IdGenerator {
    fun newId(): String
}

class DefaultIdGenerator(private val clock: () -> Long = { 0L }) : IdGenerator {
    private var counter = 0

    override fun newId(): String {
        counter++
        return "id-${clock()}-$counter-${Random.nextInt(0, Int.MAX_VALUE)}"
    }
}

data class TimetableCellView(
    val course: Course,
    val slots: List<TimetableSlot>,
    val sessions: List<CourseSession>
)

data class TimetableScreenState(
    val termId: String?,
    val days: List<Weekday>,
    val periods: List<Int>,
    val cells: Map<TimetableSlot, TimetableCellView>,
    val unscheduled: List<Course>,
    val issues: List<TimetableIssue>,
    val totalCredits: Int
)

data class AttendanceScreenState(
    val course: Course,
    val summary: AttendanceSummary,
    val records: List<AttendanceRecord>,
    val sessions: List<CourseSession>
)

data class GpaCourseRow(
    val course: Course,
    val grade: CourseGrade?,
    val included: Boolean,
    val effectiveGrade: GpaGrade?
)

data class GpaScreenState(
    val base: GpaSummary,
    val scenario: GpaScenario?,
    val scenarioSummary: GpaSummary?,
    val rows: List<GpaCourseRow>
)

data class CourseDetailView(
    val course: Course,
    val sessions: List<CourseSession>,
    val tasks: List<CourseTask>,
    val attendance: AttendanceSummary,
    val grade: CourseGrade?
)

/**
 * 画面に公開する API。
 *
 * 画面はこのクラスだけを参照し、DB や SQLDelight を直接触らない。
 * 状態は `state` と `settings` の Flow で購読し、変更系はメソッドを呼ぶ。
 */
class CoreApi(
    private val repository: CoreRepository,
    private val idGenerator: IdGenerator = DefaultIdGenerator(),
    private val clock: () -> Long = { 0L },
    syllabus: List<SyllabusEntry> = emptyList()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val _state = MutableStateFlow(repository.loadAll())
    val state: StateFlow<UserData> = _state.asStateFlow()

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val catalog = SyllabusCatalog(syllabus)
    private val _syllabusCount = MutableStateFlow(catalog.size)
    val syllabusCount: StateFlow<Int> = _syllabusCount.asStateFlow()
    val syllabusSize: Int get() = catalog.size

    val snapshot: UserData get() = _state.value

    private val store = CoreStore(
        repository = repository,
        clock = clock,
        ids = idGenerator,
        json = json,
        settings = _settings,
        catalog = catalog,
        syllabusCount = _syllabusCount,
        stateProvider = { _state.value },
        changed = { _state.value = repository.loadAll() }
    )

    /**
     * 機能ごとの入口。新しい画面は必要なサービスだけを受け取る。
     * 下の委譲メソッドは既存の呼び出しのために残している（新規追加はしない）。
     */
    val courses: CourseService = CourseService(store)
    val attendanceLog: AttendanceService = AttendanceService(store)
    val tasks: TaskService = TaskService(store)
    val grades: GradeService = GradeService(store)
    val schedule: ScheduleService = ScheduleService(store)
    val syllabusIndex: SyllabusService = SyllabusService(store)
    val settingsStore: SettingsService = SettingsService(store)
    val backup: BackupService = BackupService(store)

    private val courseService = courses
    private val attendanceService = attendanceLog
    private val taskService = tasks
    private val gradeService = grades
    private val scheduleService = schedule
    private val syllabusService = syllabusIndex
    private val settingsService = settingsStore
    private val backupService = backup

    // ----- 科目と授業回 -----

    fun saveTerm(term: Term): Term = courseService.saveTerm(term)

    fun deleteTerm(termId: String) = courseService.deleteTerm(termId)

    fun saveCourse(course: Course, today: String? = null): Course =
        courseService.saveCourse(course, today)

    /** 今日から見た既定の学期 ID（無ければ作る）。 */
    fun resolveTermId(today: String): String? =
        courseService.resolveTermId(today)

    /** その科目の授業回を作り直し、追加した件数を返す。 */
    fun rebuildSessions(courseId: String): Int =
        courseService.rebuildSessions(courseId)

    fun deleteCourse(courseId: String) = courseService.deleteCourse(courseId)

    fun saveSession(session: CourseSession): CourseSession = courseService.saveSession(session)

    fun cancelSession(sessionId: String) = courseService.cancelSession(sessionId)

    fun addMakeupSession(
        courseId: String,
        date: String,
        startPeriod: Int? = null,
        startTime: String? = null,
        endTime: String? = null,
        room: String? = null,
        replacesSessionId: String? = null
    ): CourseSession = courseService.addMakeupSession(
        courseId = courseId,
        date = date,
        startPeriod = startPeriod,
        startTime = startTime,
        endTime = endTime,
        room = room,
        replacesSessionId = replacesSessionId
    )

    // ----- 出席 -----

    fun setAttendance(
        courseId: String,
        status: AttendanceStatus,
        sessionId: String? = null,
        date: String? = null
    ): AttendanceRecord = attendanceService.setAttendance(courseId, status, sessionId, date)

    fun removeAttendance(courseId: String, sessionId: String? = null, date: String? = null) =
        attendanceService.removeAttendance(courseId, sessionId, date)

    fun attendance(courseId: String): AttendanceScreenState = attendanceService.attendance(courseId)

    // ----- 課題と学事 -----

    fun saveTask(task: CourseTask): CourseTask = taskService.saveTask(task)

    fun setTaskStatus(taskId: String, status: TaskStatus) =
        taskService.setTaskStatus(taskId, status)

    fun deleteTask(taskId: String) = taskService.deleteTask(taskId)

    fun saveSchoolEvent(event: SchoolEvent): SchoolEvent = taskService.saveSchoolEvent(event)

    fun deleteSchoolEvent(eventId: String) = taskService.deleteSchoolEvent(eventId)

    // ----- 成績 -----

    fun saveGrade(grade: CourseGrade): CourseGrade = gradeService.saveGrade(grade)

    fun deleteGrade(courseId: String) = gradeService.deleteGrade(courseId)

    fun saveScenario(name: String, overrides: Map<String, GpaGrade>): GpaScenario =
        gradeService.saveScenario(name, overrides)

    fun deleteScenario(scenarioId: String) = gradeService.deleteScenario(scenarioId)

    fun gpa(scenarioId: String? = null): GpaScreenState = gradeService.gpa(scenarioId)

    // ----- 時間割・予定・科目詳細 -----

    fun timetable(termId: String? = null): TimetableScreenState = scheduleService.timetable(termId)

    fun agenda(startDate: String, endDate: String): List<AgendaEntry> =
        scheduleService.agenda(startDate, endDate)

    fun courseDetail(courseId: String): CourseDetailView = scheduleService.courseDetail(courseId)

    // ----- シラバス・設定・バックアップ -----

    fun setSyllabus(entries: List<SyllabusEntry>) = syllabusService.setSyllabus(entries)

    fun searchSyllabus(
        query: String,
        year: Int? = null,
        weekday: org.openyaske.core.model.Weekday? = null,
        period: Int? = null,
        programType: String? = null,
        limit: Int = 30
    ): List<SyllabusSearchHit> =
        syllabusService.searchSyllabus(query, year, weekday, period, programType, limit)

    fun updateSettings(transform: (AppSettings) -> AppSettings): AppSettings =
        settingsService.updateSettings(transform)

    fun exportJson(): String = backupService.exportJson()

    fun importJson(raw: String): NormalizeReport = backupService.importJson(raw)

    /** 学習データを空にし、設定も既定へ戻す。 */
    fun clearAllData() {
        backupService.clearAll()
        updateSettings { AppSettings() }
    }

    private fun readSettings(): AppSettings {
        val raw = repository.loadSettingsJson() ?: return AppSettings()
        return runCatching {
            json.decodeFromString(AppSettings.serializer(), raw)
        }.getOrElse { AppSettings() }
    }
}

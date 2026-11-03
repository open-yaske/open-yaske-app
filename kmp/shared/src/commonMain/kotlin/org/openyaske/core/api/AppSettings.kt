package org.openyaske.core.api

import kotlinx.serialization.Serializable
import org.openyaske.core.logic.AttendancePolicy
import org.openyaske.core.logic.GradeScheme
import org.openyaske.core.logic.PeriodTimes
import org.openyaske.core.model.Weekday

/** 時間割セルの透過モード。 */
@Serializable
enum class CellSurfaceMode {
    /** 現在のセル塗り。 */
    OPAQUE,

    /** 面を 55% にする。ブラーは使わず、細い境界線で表現する。 */
    FROSTED,

    /** 面を 30%。 */
    TRANSLUCENT,

    /** 塗りなし。枠線だけ。 */
    TRANSPARENT
}

/** アクセントの作り方。MONOCHROME のときは色相を使わない。 */
@Serializable
enum class AccentStyle {
    COLOR,
    MONOCHROME
}

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,

    /** 有機 EL 向けの純黒。背景を #000 にする。 */
    PURE_BLACK
}

@Serializable
data class NotificationSettings(
    val enabled: Boolean = true,
    /** 授業開始の何分前に通知するか。 */
    val leadMinutes: Int = 10,
    /** 課題の締切前日 20:00 に知らせる。 */
    val taskReminderEnabled: Boolean = true,
    /** 授業中は通知を抑制する。 */
    val quietDuringClass: Boolean = true,
    /** 科目ごとの通知可否。未指定は全体設定に従う。 */
    val perCourse: Map<String, Boolean> = emptyMap()
)

/**
 * 画面が参照する設定。
 *
 * 設定は 1 つの型に集約し、画面ごとに別々の保存先を持たない。
 * JSON で保存するため、項目を増やしてもマイグレーションは
 * `schemaVersion` と既定値で吸収できる。
 */
@Serializable
data class AppSettings(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val locale: String = "ja",
    val weekStart: Weekday = Weekday.MON,
    val visibleWeekdays: List<Weekday> = Weekday.weekOrder.take(6),
    val maxPeriod: Int = 5,
    val periodStartMinutes: Map<Int, Int> = PeriodTimes.uecStartMinutes,
    val attendancePolicy: AttendancePolicy = AttendancePolicy(),
    val notifications: NotificationSettings = NotificationSettings(),
    val calendarShowClasses: Boolean = true,
    val calendarShowTasks: Boolean = true,
    val calendarShowEvents: Boolean = true,
    val gradeScheme: GradeScheme = GradeScheme.SABCD,
    /** アクセントの色相（0..359）。既定は藍。 */
    val accentHue: Int = 230,
    /** アクセントの作り方（カラー / モノクロ）。 */
    val accentStyle: AccentStyle = AccentStyle.COLOR,
    /** 時間割セルの透過モード。null は「背景ありならすりガラス、なしなら不透明」。 */
    val timetableCellMode: CellSurfaceMode? = null
) {
    fun normalized(): AppSettings {
        val days = visibleWeekdays.distinct()
            .ifEmpty { Weekday.weekOrder.take(6) }
            .sortedBy { it.isoNumber }
        val periods = periodStartMinutes
            .filterKeys { it in 1..7 }
            .mapValues { (_, value) -> value.coerceIn(0, 23 * 60 + 59) }
            .ifEmpty { PeriodTimes.uecStartMinutes }
        val policy = attendancePolicy.copy(
            lateWeight = attendancePolicy.lateWeight.coerceIn(0.0, 1.0),
            excusedWeight = attendancePolicy.excusedWeight.coerceIn(0.0, 1.0),
            absenceWarnRatio = attendancePolicy.absenceWarnRatio.coerceIn(0.1, 0.5)
        )
        return copy(
            theme = theme,
            accentHue = ((accentHue % 360) + 360) % 360,
            locale = locale.ifBlank { "ja" },
            visibleWeekdays = days,
            maxPeriod = maxPeriod.coerceIn(1, 7),
            periodStartMinutes = periods,
            attendancePolicy = policy,
            notifications = notifications.copy(
                leadMinutes = notifications.leadMinutes.coerceIn(0, 24 * 60),
                perCourse = notifications.perCourse.filterKeys { it.isNotBlank() }
            )
        )
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

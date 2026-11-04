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
/** 乗換案内に登録した駅。 */
@Serializable
data class TransitStation(val id: String, val name: String)

/** 駅ごとの絞り込み。 */
@Serializable
data class TransitFilter(
    val directions: List<String> = emptyList(),
    val type: String? = null
)

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
    val timetableCellMode: CellSurfaceMode? = null,
    /** ホームに乗換カードを出すか。 */
    val transitVisible: Boolean = true,
    /** 登録した駅（複数可）。 */
    val transitStations: List<TransitStation> = emptyList(),
    /** ホームに出す駅。複数選べる。 */
    val transitStationIds: List<String> = emptyList(),
    /** 旧形式の単一駅。読み込み時に [transitStationIds] へ畳む。 */
    val transitStationId: String? = null,
    /** 駅ごとの方面・種別の絞り込み。 */
    val transitFilters: Map<String, TransitFilter> = emptyMap(),
    /** 旧形式の方面（全体）。読み込み時に駅ごとへ畳む。 */
    val transitDirections: List<String> = emptyList(),
    /** 旧形式の単一方面。読み込み時に駅ごとへ畳む。 */
    val transitDirection: String? = null,
    /** 旧形式の種別（全体）。読み込み時に駅ごとへ畳む。 */
    val transitType: String? = null,
    /** 路線の絞り込み。 */
    val transitLine: String? = null,
    /** ホームに天気カードを出すか。 */
    val weatherVisible: Boolean = true,
    /** 天気を取る地点の表示名。 */
    val weatherPlaceName: String = "調布キャンパス",
    val weatherLatitude: Double = 35.6256,
    val weatherLongitude: Double = 139.4855
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
        val stations = transitStations
            .filter { it.id.isNotBlank() && it.name.isNotBlank() }
            .distinctBy { it.id }
        val stationIds = (transitStationIds + listOfNotNull(transitStationId))
            .filter { id -> stations.any { it.id == id } }
            .distinct()
        val legacyDirections = (transitDirections + listOfNotNull(transitDirection))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        val legacyType = transitType?.takeIf { it.isNotBlank() }
        val cleanedFilters = transitFilters
            .filterKeys { key -> stations.any { it.id == key } }
            .mapValues { (_, filter) ->
                TransitFilter(
                    directions = filter.directions
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct(),
                    type = filter.type?.takeIf { it.isNotBlank() }
                )
            }
            .filterValues { it.directions.isNotEmpty() || it.type != null }
        // 旧形式の全体設定は、選択中の駅すべてに配る。
        val filters = if (cleanedFilters.isEmpty() &&
            (legacyDirections.isNotEmpty() || legacyType != null)
        ) {
            stationIds.associateWith { TransitFilter(legacyDirections, legacyType) }
        } else {
            cleanedFilters
        }
        return copy(
            theme = theme,
            accentHue = ((accentHue % 360) + 360) % 360,
            transitStations = stations,
            transitStationIds = stationIds,
            transitStationId = null,
            transitFilters = filters,
            transitDirections = emptyList(),
            transitDirection = null,
            transitType = null,
            transitLine = transitLine?.takeIf { it.isNotBlank() },
            weatherPlaceName = weatherPlaceName.ifBlank { "調布キャンパス" },
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

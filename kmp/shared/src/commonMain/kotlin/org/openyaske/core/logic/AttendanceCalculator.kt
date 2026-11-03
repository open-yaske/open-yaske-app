package org.openyaske.core.logic

import kotlinx.serialization.Serializable
import org.openyaske.core.model.AttendanceRecord
import org.openyaske.core.model.AttendanceStatus

/**
 * 出席率の計算方針。
 *
 * 遅刻の扱いは実装ごとに揺れやすい。Svelte 版は 0.5 回、APK 版は 1 回として数えていた。
 * ここでは係数を明示し、既定値を Svelte 版（0.5）に合わせる。
 */
@Serializable
data class AttendancePolicy(
    val lateWeight: Double = 0.5,
    val excusedWeight: Double = 1.0,
    /** 休講を分母に含めるか。通常は false（休講は授業がなかった扱い）。 */
    val includeCancelledInDenominator: Boolean = false,
    /**
     * 欠席がこの割合に達したら警告する。0.1..0.5。
     * 分母は 出席 + 欠席 + 遅刻×重み + 公欠×重み（休講は含めない）。
     */
    val absenceWarnRatio: Double = 1.0 / 3.0
) {
    init {
        require(lateWeight in 0.0..1.0) { "lateWeight must be 0..1" }
        require(excusedWeight in 0.0..1.0) { "excusedWeight must be 0..1" }
        require(absenceWarnRatio in 0.1..0.5) { "absenceWarnRatio must be 0.1..0.5" }
    }
}

data class AttendanceSummary(
    val present: Int,
    val absent: Int,
    val late: Int,
    val excused: Int,
    val cancelled: Int,
    val total: Int,
    /** 0..100 */
    val rate: Double,
    /** 欠席がこの回数に達すると警告（1/3 基準の上限）。 */
    val absenceLimit: Int,
    /** 欠席 / 分母 が方針の割合以上か。 */
    val atRisk: Boolean
)

object AttendanceCalculator {
    /**
     * 1 科目分の記録を集計する。
     * 同じ日付の重複は `recordedAtMillis` が新しい方を採用する。
     */
    fun summarize(
        records: List<AttendanceRecord>,
        policy: AttendancePolicy = AttendancePolicy()
    ): AttendanceSummary {
        val perDate = linkedMapOf<String, AttendanceRecord>()
        for (record in records) {
            val existing = perDate[record.date]
            if (existing == null || record.recordedAtMillis >= existing.recordedAtMillis) {
                perDate[record.date] = record
            }
        }

        var present = 0
        var absent = 0
        var late = 0
        var excused = 0
        var cancelled = 0

        for (record in perDate.values) {
            when (record.status) {
                AttendanceStatus.PRESENT -> present++
                AttendanceStatus.ABSENT -> absent++
                AttendanceStatus.LATE -> late++
                AttendanceStatus.EXCUSED -> excused++
                AttendanceStatus.CANCELLED -> cancelled++
            }
        }

        val denominator = present + absent + late + excused + if (policy.includeCancelledInDenominator) cancelled else 0
        val effective = present + late * policy.lateWeight + excused * policy.excusedWeight
        val rate = if (denominator == 0) 0.0 else (effective / denominator * 100.0).coerceIn(0.0, 100.0)

        // 警告は「欠席 / (出席 + 欠席 + 遅刻×重み + 公欠×重み)」。休講は分母に入れない。
        val weightedOthers = present + late * policy.lateWeight + excused * policy.excusedWeight
        val warnBase = weightedOthers + absent
        val absentRatio = if (warnBase == 0.0) 0.0 else absent / warnBase
        val atRisk = warnBase > 0.0 && absentRatio >= policy.absenceWarnRatio
        val ratio = policy.absenceWarnRatio
        // 警告が始まる欠席回数。others に対して r/(1-r) を超えた最初の整数。
        val limit = if (weightedOthers <= 0.0) {
            0
        } else {
            kotlin.math.ceil(ratio * weightedOthers / (1.0 - ratio)).toInt().coerceAtLeast(0)
        }

        return AttendanceSummary(
            present = present,
            absent = absent,
            late = late,
            excused = excused,
            cancelled = cancelled,
            total = denominator,
            rate = rate,
            absenceLimit = limit,
            atRisk = atRisk
        )
    }

    /** 科目 ID ごとに集計する。 */
    fun summarizeByCourse(
        records: List<AttendanceRecord>,
        policy: AttendancePolicy = AttendancePolicy()
    ): Map<String, AttendanceSummary> =
        records.groupBy { it.courseId }
            .mapValues { (_, courseRecords) -> summarize(courseRecords, policy) }
}

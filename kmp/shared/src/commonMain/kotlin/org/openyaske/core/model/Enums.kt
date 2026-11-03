package org.openyaske.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 曜日。ISO-8601 と同じく月曜=1 … 日曜=7。 */
@Serializable
enum class Weekday(val isoNumber: Int) {
    @SerialName("mon")
    MON(1),

    @SerialName("tue")
    TUE(2),

    @SerialName("wed")
    WED(3),

    @SerialName("thu")
    THU(4),

    @SerialName("fri")
    FRI(5),

    @SerialName("sat")
    SAT(6),

    @SerialName("sun")
    SUN(7);

    companion object {
        fun fromIsoNumber(number: Int): Weekday? = entries.firstOrNull { it.isoNumber == number }

        /** 月曜始まりの並び。時間割の列順に使う。 */
        val weekOrder: List<Weekday> = listOf(MON, TUE, WED, THU, FRI, SAT, SUN)
    }
}

/** 学期。UEC は前学期 / 後期の 2 学期制。 */
@Serializable
enum class Semester {
    @SerialName("spring")
    SPRING,

    @SerialName("fall")
    FALL
}

/** 科目区分。教職（TEACHING）は APK 版だけが持っていた区分。 */
@Serializable
enum class CourseType {
    @SerialName("required")
    REQUIRED,

    @SerialName("elective")
    ELECTIVE,

    @SerialName("free")
    FREE,

    @SerialName("teaching")
    TEACHING
}

/**
 * 出席状態。
 *
 * 休講（CANCELLED）を独立テーブルではなく同じ列挙に含める。
 * APK 版は `attendance_records` と `cancelled_classes` の 2 テーブルに分けており、
 * 同じ「休講」を 2 か所で数える余地があった。
 */
@Serializable
enum class AttendanceStatus {
    @SerialName("present")
    PRESENT,

    @SerialName("absent")
    ABSENT,

    @SerialName("late")
    LATE,

    @SerialName("excused")
    EXCUSED,

    @SerialName("cancelled")
    CANCELLED
}

@Serializable
enum class TodoPriority {
    @SerialName("low")
    LOW,

    @SerialName("medium")
    MEDIUM,

    @SerialName("high")
    HIGH
}

@Serializable
enum class TodoCategory {
    @SerialName("assignment")
    ASSIGNMENT,

    @SerialName("exam")
    EXAM,

    @SerialName("report")
    REPORT,

    @SerialName("other")
    OTHER
}

@Serializable
enum class RepeatPattern {
    @SerialName("none")
    NONE,

    @SerialName("daily")
    DAILY,

    @SerialName("weekly")
    WEEKLY,

    @SerialName("biweekly")
    BIWEEKLY,

    @SerialName("monthly")
    MONTHLY
}

/** 成績評価。単位数 × ポイントで GPA を出す。 */
@Serializable
enum class GpaGrade(val point: Double) {
    @SerialName("S")
    S(4.0),

    @SerialName("A")
    A(3.0),

    @SerialName("B")
    B(2.0),

    @SerialName("C")
    C(1.0),

    @SerialName("F")
    F(0.0)
}

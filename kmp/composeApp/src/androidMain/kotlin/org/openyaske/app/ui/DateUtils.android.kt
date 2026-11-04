package org.openyaske.app.ui

actual fun todayIso(): String = java.time.LocalDate.now().toString()

actual fun nowMinutes(): Int {
    val time = java.time.LocalTime.now()
    return time.hour * 60 + time.minute
}

actual fun nowMillis(): Long = System.currentTimeMillis()

actual fun nowSecondOfDay(): Int {
    val calendar = java.util.Calendar.getInstance()
    return calendar.get(java.util.Calendar.HOUR_OF_DAY) * 3600 +
        calendar.get(java.util.Calendar.MINUTE) * 60 +
        calendar.get(java.util.Calendar.SECOND)
}

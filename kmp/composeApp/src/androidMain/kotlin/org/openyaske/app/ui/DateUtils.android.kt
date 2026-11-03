package org.openyaske.app.ui

actual fun todayIso(): String = java.time.LocalDate.now().toString()

actual fun nowMinutes(): Int {
    val time = java.time.LocalTime.now()
    return time.hour * 60 + time.minute
}

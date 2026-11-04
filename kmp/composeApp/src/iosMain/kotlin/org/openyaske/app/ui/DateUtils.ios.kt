package org.openyaske.app.ui

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale

actual fun nowMinutes(): Int {
    val calendar = platform.Foundation.NSCalendar.currentCalendar
    val components = calendar.components(
        platform.Foundation.NSCalendarUnitHour or platform.Foundation.NSCalendarUnitMinute,
        fromDate = NSDate()
    )
    return (components.hour.toInt() * 60) + components.minute.toInt()
}

actual fun todayIso(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd"
    formatter.locale = NSLocale(localeIdentifier = "en_US_POSIX")
    return formatter.stringFromDate(NSDate())
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
actual fun nowMillis(): Long = platform.posix.time(null) * 1000L

actual fun nowSecondOfDay(): Int {
    val calendar = platform.Foundation.NSCalendar.currentCalendar
    val components = calendar.components(
        platform.Foundation.NSCalendarUnitHour or
            platform.Foundation.NSCalendarUnitMinute or
            platform.Foundation.NSCalendarUnitSecond,
        fromDate = NSDate()
    )
    return components.hour.toInt() * 3600 +
        components.minute.toInt() * 60 +
        components.second.toInt()
}

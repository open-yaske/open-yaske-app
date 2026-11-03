package org.openyaske.app.ui

import androidx.compose.ui.graphics.vector.ImageVector
import org.openyaske.app.ui.design.AppIcons

enum class Destination(val labelKey: String, val icon: ImageVector) {
    HOME("nav_home", AppIcons.home),
    TASKS("nav_tasks", AppIcons.tasks),
    TIMETABLE("nav_timetable", AppIcons.timetable),
    CALENDAR("nav_calendar", AppIcons.calendar),
    MORE("nav_more", AppIcons.more)
}

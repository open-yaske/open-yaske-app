package org.openyaske.app.ui.design

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * アイコンの唯一の出どころ。
 *
 * 同じ意味に複数の絵柄が混ざらないよう、画面はここだけを見る。
 * 追加するときは、近い意味の既存項目がないか先に確認する。
 */
object AppIcons {
    val home: ImageVector = Icons.Filled.Home
    val tasks: ImageVector = Icons.Filled.Checklist
    val timetable: ImageVector = Icons.Filled.CalendarViewWeek
    val calendar: ImageVector = Icons.Filled.CalendarMonth
    val search: ImageVector = Icons.Filled.Search
    val notifications: ImageVector = Icons.Filled.Notifications
    val settings: ImageVector = Icons.Filled.Settings
    val grades: ImageVector = Icons.Filled.School
    val more: ImageVector = Icons.Filled.MoreHoriz
    val pdf: ImageVector = Icons.Filled.PictureAsPdf
    val license: ImageVector = Icons.Filled.Description

    val add: ImageVector = Icons.Filled.Add
    val edit: ImageVector = Icons.Filled.Edit
    val check: ImageVector = Icons.Filled.Check
    val back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val disclosure: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight
    val expand: ImageVector = Icons.Filled.ExpandMore
    val collapse: ImageVector = Icons.Filled.ExpandLess
    val previous: ImageVector = Icons.Filled.ChevronLeft
    val next: ImageVector = Icons.Filled.ChevronRight
    val dropdown: ImageVector = Icons.Filled.ArrowDropDown
    val tune: ImageVector = Icons.Filled.Tune

    /** 時刻から選ぶ空の絵。天気そのものではなく、朝昼夜の目印。 */
    val day: ImageVector = Icons.Filled.WbSunny
    val night: ImageVector = Icons.Filled.NightsStay
}

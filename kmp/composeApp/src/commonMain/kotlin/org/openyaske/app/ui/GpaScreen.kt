package org.openyaske.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.openyaske.app.ui.design.tabular
import org.openyaske.app.ui.design.AppCard
import org.openyaske.app.ui.design.HairLine
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.AppTopBar
import org.openyaske.app.ui.design.SectionBar
import org.openyaske.app.ui.design.Space
import org.openyaske.core.api.CoreApi
import org.openyaske.core.model.CourseGrade
import org.openyaske.core.model.GpaGrade

@Composable
fun GpaScreen(api: CoreApi, onClose: () -> Unit) {
    val data by api.state.collectAsState()
    val settings by api.settings.collectAsState()
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    val state = remember(data) { api.gpa() }
    val scheme = settings.gradeScheme

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        AppTopBar(title = t["gpa_title"], onBack = onClose, modifier = Modifier.widthIn(max = 760.dp))
        LazyColumn(
            modifier = Modifier.widthIn(max = 760.dp).fillMaxHeight(),
            contentPadding = PaddingValues(start = Space.lg, end = Space.lg, bottom = Space.xxl),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            item {
                AppCard(color = colors.primaryContainer, border = false) {
                    Row(modifier = Modifier.padding(Space.lg)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t["gpa_total"], style = MaterialTheme.typography.labelSmall, color = colors.onPrimaryContainer)
                            Text(
                                text = ((state.base.gpa * 100).roundToInt() / 100.0).toString(),
                                style = MaterialTheme.typography.headlineLarge.tabular(),
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t["gpa_credits"], style = MaterialTheme.typography.labelSmall, color = colors.onPrimaryContainer)
                            Text(
                                text = "${state.base.earnedCredits}",
                                style = MaterialTheme.typography.headlineLarge.tabular(),
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primary
                            )
                        }
                    }
                }
            }

            item { SectionBar(t["gpa_courses"]) }
            item {
                AppCard {
                    Column {
                        if (state.rows.isEmpty()) {
                            Text(
                                t["gpa_no_data"],
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(Space.md)
                            )
                        }
                        state.rows.forEachIndexed { index, row ->
                            if (index > 0) HairLine(modifier = Modifier.padding(start = Space.md))
                            GradeRow(
                                api = api,
                                courseId = row.course.id,
                                name = row.course.name,
                                credits = row.course.credits,
                                grade = row.effectiveGrade,
                                scheme = scheme
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GradeRow(
    api: CoreApi,
    courseId: String,
    name: String,
    credits: Int,
    grade: GpaGrade?,
    scheme: org.openyaske.core.logic.GradeScheme
) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { menu = true }
                .padding(horizontal = Space.md, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = colors.text)
                Text(
                    "$credits ${t["timetable_credit_unit"]}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
            Text(
                text = grade?.let { org.openyaske.core.logic.GradeCatalog.label(it, scheme) }
                    ?: t["grade_not_set"],
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (grade == null) colors.textTertiary else colors.primary
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            org.openyaske.core.logic.GradeCatalog.options(scheme).forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "${option.label}   ${org.openyaske.core.logic.GradeCatalog.formatPoint(option.point)}"
                        )
                    },
                    onClick = {
                        api.saveGrade(
                            CourseGrade(
                                courseId = courseId,
                                state = org.openyaske.core.model.GradeState.GRADED,
                                grade = option.grade
                            )
                        )
                        menu = false
                    }
                )
            }
        }
    }
}

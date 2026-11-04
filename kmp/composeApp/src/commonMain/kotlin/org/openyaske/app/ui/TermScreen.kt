package org.openyaske.app.ui

import org.openyaske.app.ui.design.tabular
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
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
import org.openyaske.app.ui.design.AppCard
import org.openyaske.app.ui.design.AppIcons
import org.openyaske.app.ui.design.AppTopBar
import org.openyaske.app.ui.design.Disclosure
import org.openyaske.app.ui.design.HairLine
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.SectionBar
import org.openyaske.app.ui.design.Space
import org.openyaske.core.api.CoreApi
import org.openyaske.core.model.Term

/**
 * 学期の一覧と編集。学期を変えると、その学期の科目の授業回が作り直される。
 */
@Composable
fun TermScreen(api: CoreApi, onBack: () -> Unit) {
    val data by api.state.collectAsState()
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    var editing by remember { mutableStateOf<Term?>(null) }
    var creating by remember { mutableStateOf(false) }

    val terms = remember(data.terms) {
        data.terms.sortedWith(compareBy({ it.year }, { it.season?.name ?: "Z" }))
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        AppTopBar(title = t["term_manage"], onBack = onBack, modifier = Modifier.widthIn(max = 760.dp))
        LazyColumn(
            modifier = Modifier.widthIn(max = 760.dp).fillMaxHeight(),
            contentPadding = PaddingValues(
                start = Space.lg,
                end = Space.lg,
                bottom = Space.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            item { SectionBar(t["term_manage"]) }
            if (terms.isEmpty()) {
                item {
                    AppCard {
                        Text(
                            text = t["term_empty"],
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(Space.lg)
                        )
                        Text(
                            text = t["term_required_hint"],
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = Space.lg).padding(bottom = Space.lg)
                        )
                    }
                }
            } else {
                item {
                    AppCard {
                        Column {
                            terms.forEachIndexed { index, term ->
                                if (index > 0) HairLine(modifier = Modifier.padding(start = Space.lg))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { editing = term }
                                        .padding(horizontal = Space.lg, vertical = Space.md),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = termLabel(term, t["lang"]),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.text
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${term.startDate} 〜 ${term.endDate}",
                                                style = MaterialTheme.typography.labelSmall.tabular(),
                                                color = colors.textSecondary
                                            )
                                            if (!org.openyaske.core.logic.AcademicCalendar.isVerified(term.year)) {
                                                Text(
                                                    text = t["term_provisional"],
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.warning,
                                                    modifier = Modifier.padding(start = Space.sm)
                                                )
                                            }
                                        }
                                    }
                                    Disclosure()
                                }
                            }
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { creating = true }
                        .padding(vertical = Space.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        AppIcons.add,
                        contentDescription = null,
                        tint = colors.primary
                    )
                    Text(
                        text = t["term_add"],
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.primary,
                        modifier = Modifier.padding(start = Space.sm)
                    )
                }
            }
        }
    }

    if (creating || editing != null) {
        TermEditorSheet(
            api = api,
            term = if (creating) null else editing,
            onDone = {
                creating = false
                editing = null
            },
            onDismiss = {
                creating = false
                editing = null
            }
        )
    }
}

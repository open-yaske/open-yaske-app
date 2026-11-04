package org.openyaske.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.openyaske.app.ui.design.AppCard
import org.openyaske.app.ui.design.FilterPill
import org.openyaske.app.ui.design.HairLine
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.Space
import org.openyaske.core.api.AppSettings
import org.openyaske.core.api.CoreApi
import org.openyaske.core.api.TransitBoard
import org.openyaske.core.api.TransitClient
import org.openyaske.core.api.TransitFilter
import org.openyaske.core.api.TransitPlace
import org.openyaske.core.api.TransitStation

/**
 * 設定の乗換案内。駅（複数）、ホームに出すか、駅ごとの方面・種別をここで決める。
 * カード側には選択 UI を置かない。
 */
@Composable
internal fun TransitSettingsSection(api: CoreApi, settings: AppSettings) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<TransitPlace>>(emptyList()) }
    var boards by remember { mutableStateOf<Map<String, TransitBoard>>(emptyMap()) }
    var loading by remember { mutableStateOf(false) }
    val english = !(settings.locale.startsWith("ja") || settings.locale.startsWith("zh"))

    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        kotlinx.coroutines.delay(300)
        results = runCatching { TransitClient.suggest(query) }.getOrDefault(emptyList())
        searching = false
    }

    LaunchedEffect(settings.transitStationIds) {
        val ids = settings.transitStationIds
        if (ids.isEmpty()) {
            boards = emptyMap()
            loading = false
            return@LaunchedEffect
        }
        loading = true
        val loaded = ids.mapNotNull { id ->
            val board = TransitBoardLoader.cached(id) ?: TransitBoardLoader.load(id)
            board?.let { id to it }
        }.toMap()
        boards = loaded
        loading = false
    }

    AppCard {
        Column(modifier = Modifier.padding(Space.md)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        api.updateSettings { it.copy(transitVisible = !it.transitVisible) }
                    }
                    .padding(vertical = Space.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = t["transit_show_on_home"],
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text
                    )
                    Text(
                        text = t["transit_show_on_home_hint"],
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
                Switch(
                    checked = settings.transitVisible,
                    onCheckedChange = { enabled ->
                        api.updateSettings { it.copy(transitVisible = enabled) }
                    }
                )
            }
            HairLine(modifier = Modifier.padding(vertical = Space.xs))

            if (settings.transitStations.isEmpty()) {
                Text(
                    text = t["transit_add_hint"],
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
            settings.transitStations.forEach { station ->
                val selected = station.id in settings.transitStationIds
                StationRow(
                    station = station,
                    selected = selected,
                    onToggle = {
                        api.updateSettings { current ->
                            val ids = current.transitStationIds
                            current.copy(
                                transitStationIds = if (station.id in ids) ids - station.id
                                else ids + station.id
                            )
                        }
                    },
                    onDelete = {
                        api.updateSettings { current ->
                            current.copy(
                                transitStations = current.transitStations
                                    .filterNot { it.id == station.id },
                                transitStationIds = current.transitStationIds - station.id,
                                transitFilters = current.transitFilters - station.id
                            )
                        }
                    }
                )
                if (selected) {
                    val board = boards[station.id]
                    if (board == null) {
                        if (loading) LoadingRow(t["loading"])
                    } else {
                        StationFilterBlock(
                            api = api,
                            stationId = station.id,
                            board = board,
                            filter = settings.transitFilters[station.id] ?: TransitFilter(),
                            english = english
                        )
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(t["transit_search"]) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = Space.sm)
            )
            if (searching) {
                LoadingRow(t["searching"])
            }
            results.forEach { place ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            api.updateSettings { current ->
                                current.copy(
                                    transitStations = current.transitStations + TransitStation(
                                        id = place.id,
                                        name = place.name
                                    ),
                                    transitStationIds = current.transitStationIds + place.id
                                )
                            }
                            query = ""
                        }
                        .padding(vertical = Space.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = t["common_add"],
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary
                    )
                }
            }
        }
    }
}

/** 1 駅分の方面・種別。その駅のボードから選択肢を作る。 */
@Composable
private fun StationFilterBlock(
    api: CoreApi,
    stationId: String,
    board: TransitBoard,
    filter: TransitFilter,
    english: Boolean
) {
    val t = LocalUiStrings.current
    val destinations = board.departures
        .map { it.headsign.destination }
        .distinctBy { it.ja }
        .sortedBy { it.ja }
        .map { it.label(english) to it.ja }
    val types = board.departures
        .mapNotNull { it.headsign.serviceType }
        .distinctBy { it.ja }
        .sortedBy { it.ja }
        .map { it.label(english) to it.ja }

    fun update(next: TransitFilter) {
        api.updateSettings { current ->
            current.copy(transitFilters = current.transitFilters + (stationId to next))
        }
    }

    if (destinations.size > 1) {
        MultiChoiceRow(
            label = t["transit_destination"],
            options = destinations,
            selected = filter.directions.toSet(),
            onToggle = { key ->
                val values = filter.directions
                update(
                    filter.copy(
                        directions = if (key in values) values - key else values + key
                    )
                )
            },
            onClear = { update(filter.copy(directions = emptyList())) }
        )
    }
    if (types.size > 1) {
        ChoiceRow(
            label = t["transit_type"],
            options = types,
            selected = filter.type,
            onSelect = { key -> update(filter.copy(type = key)) }
        )
    }
}

@Composable
private fun LoadingRow(text: String) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs)
    ) {
        LoadingIndicator(modifier = Modifier.size(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun StationRow(
    station: TransitStation,
    selected: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 複数選べることが分かるように、選択中はチェックを出す。
        Text(
            text = if (selected) "✓" else "○",
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) colors.primary else colors.textTertiary
        )
        Text(
            text = station.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) colors.primary else colors.text,
            modifier = Modifier.weight(1f).padding(start = Space.sm)
        )
        TextButton(onClick = onDelete) {
            Text(t["common_delete"], color = colors.danger)
        }
    }
}

/** 「すべて」＋複数選択。押すたびに追加・解除する。 */
@Composable
private fun MultiChoiceRow(
    label: String,
    options: List<Pair<String, String>>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit
) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Space.xs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = Space.xs),
            horizontalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            FilterPill(
                text = t["transit_filter_all"],
                onClick = onClear,
                selected = selected.isEmpty()
            )
            options.forEach { (text, key) ->
                FilterPill(
                    text = text,
                    onClick = { onToggle(key) },
                    selected = key in selected
                )
            }
        }
    }
}

/** 「すべて」＋単一選択。 */
@Composable
private fun ChoiceRow(
    label: String,
    options: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Space.xs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = Space.xs),
            horizontalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            FilterPill(
                text = t["transit_filter_all"],
                onClick = { onSelect(null) },
                selected = selected == null
            )
            options.forEach { (text, key) ->
                FilterPill(
                    text = text,
                    onClick = { onSelect(if (selected == key) null else key) },
                    selected = selected == key
                )
            }
        }
    }
}

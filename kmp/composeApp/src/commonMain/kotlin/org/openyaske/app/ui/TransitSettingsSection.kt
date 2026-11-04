package org.openyaske.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import org.openyaske.app.ui.design.AppCard
import org.openyaske.app.ui.design.FilterPill
import org.openyaske.app.ui.design.HairLine
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.Space
import org.openyaske.core.api.AppSettings
import org.openyaske.core.api.CoreApi
import org.openyaske.core.api.TransitBoard
import org.openyaske.core.api.TransitClient
import org.openyaske.core.api.TransitPlace
import org.openyaske.core.api.TransitStation

/**
 * 設定の乗換案内。駅（場所）、ホームに出すか、方面と種別の絞り込みをここで決める。
 * カード側には選択 UI を置かない。
 */
@Composable
internal fun TransitSettingsSection(api: CoreApi, settings: AppSettings) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<TransitPlace>>(emptyList()) }
    var board by remember { mutableStateOf<TransitBoard?>(null) }
    val english = !(settings.locale.startsWith("ja") || settings.locale.startsWith("zh"))

    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(300)
        results = runCatching { TransitClient.suggest(query) }.getOrDefault(emptyList())
    }

    LaunchedEffect(settings.transitStationId) {
        val stationId = settings.transitStationId
        if (stationId == null) {
            board = null
            return@LaunchedEffect
        }
        board = TransitBoardLoader.cached(stationId)
            ?: TransitBoardLoader.load(stationId)
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
                StationRow(
                    station = station,
                    selected = station.id == settings.transitStationId,
                    onSelect = {
                        // 駅を変えたら方面・種別の絞り込みは解除する。
                        api.updateSettings {
                            it.copy(
                                transitStationId = station.id,
                                transitDirection = null,
                                transitType = null
                            )
                        }
                    },
                    onDelete = {
                        api.updateSettings { current ->
                            current.copy(
                                transitStations = current.transitStations
                                    .filterNot { it.id == station.id },
                                transitStationId = if (current.transitStationId == station.id) null
                                else current.transitStationId
                            )
                        }
                    }
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(t["transit_search"]) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = Space.sm)
            )
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
                                    transitStationId = place.id,
                                    transitDirection = null,
                                    transitType = null
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

            val current = board
            if (current != null) {
                val destinations = current.departures
                    .map { it.headsign.destination }
                    .distinctBy { it.ja }
                    .sortedBy { it.ja }
                    .map { it.label(english) to it.ja }
                val types = current.departures
                    .mapNotNull { it.headsign.serviceType }
                    .distinctBy { it.ja }
                    .sortedBy { it.ja }
                    .map { it.label(english) to it.ja }

                if (destinations.size > 1) {
                    HairLine(modifier = Modifier.padding(vertical = Space.xs))
                    ChoiceRow(
                        label = t["transit_destination"],
                        options = destinations,
                        selected = settings.transitDirection,
                        onSelect = { key ->
                            api.updateSettings { it.copy(transitDirection = key) }
                        }
                    )
                }
                if (types.size > 1) {
                    ChoiceRow(
                        label = t["transit_type"],
                        options = types,
                        selected = settings.transitType,
                        onSelect = { key ->
                            api.updateSettings { it.copy(transitType = key) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StationRow(
    station: TransitStation,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = station.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) colors.primary else colors.text,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Text(
                text = t["transit_active"],
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary
            )
        }
        TextButton(onClick = onDelete) {
            Text(t["common_delete"], color = colors.danger)
        }
    }
}

/** 「すべて」＋選択肢。選んでいるものをもう一度押すと解除する。 */
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

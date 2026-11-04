package org.openyaske.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import org.openyaske.app.ui.design.HairLine
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.Space
import org.openyaske.core.api.AppSettings
import org.openyaske.core.api.CoreApi
import org.openyaske.core.api.WeatherClient
import org.openyaske.core.api.WeatherPlace

/**
 * 設定の天気。ホームに出すかと、地点（緯度経度）を決める。
 * 地点は Open-Meteo の geocoding で名前から引く。
 */
@Composable
internal fun WeatherSettingsSection(api: CoreApi, settings: AppSettings) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<WeatherPlace>>(emptyList()) }
    val english = !(settings.locale.startsWith("ja") || settings.locale.startsWith("zh"))

    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        kotlinx.coroutines.delay(300)
        results = runCatching { WeatherClient.searchPlaces(query) }.getOrDefault(emptyList())
        searching = false
    }

    AppCard {
        Column(modifier = Modifier.padding(Space.md)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        api.updateSettings { it.copy(weatherVisible = !it.weatherVisible) }
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
                        text = t["weather_show_on_home_hint"],
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
                Switch(
                    checked = settings.weatherVisible,
                    onCheckedChange = { enabled ->
                        api.updateSettings { it.copy(weatherVisible = enabled) }
                    }
                )
            }
            HairLine(modifier = Modifier.padding(vertical = Space.xs))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Space.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = t["weather_place"],
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = settings.weatherPlaceName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(t["weather_place_search"]) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = Space.sm)
            )
            if (searching) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Space.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.xs)
                ) {
                    LoadingIndicator(modifier = Modifier.size(14.dp))
                    Text(
                        text = t["searching"],
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
            }
            results.forEach { place ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            api.updateSettings {
                                it.copy(
                                    weatherPlaceName = place.name,
                                    weatherLatitude = place.latitude,
                                    weatherLongitude = place.longitude
                                )
                            }
                            query = ""
                        }
                        .padding(vertical = Space.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = place.displayName(english),
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

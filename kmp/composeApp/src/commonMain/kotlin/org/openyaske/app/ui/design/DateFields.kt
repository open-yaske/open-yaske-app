package org.openyaske.app.ui.design

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.openyaske.app.ui.LocalUiStrings
import org.openyaske.core.validation.IsoDate

private const val MILLIS_PER_DAY = 86_400_000L

private fun isoToMillis(value: String): Long? =
    runCatching { IsoDate.toEpochDay(value) * MILLIS_PER_DAY }.getOrNull()

private fun millisToIso(millis: Long): String = IsoDate.fromEpochDay(millis / MILLIS_PER_DAY)

/**
 * 日付を選ぶ欄。押すと M3 の DatePickerDialog を開く。
 * 値が空のときはラベルを薄く出す（期限なしを許す画面で使う）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    value: String?,
    onChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    allowClear: Boolean = true
) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    var open by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { open = true },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = value ?: label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (value == null) colors.textTertiary else colors.text
        )
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = value?.let(::isoToMillis))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { onChange(millisToIso(it)) }
                        open = false
                    }
                ) { Text(t["common_save"]) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (allowClear) onChange(null)
                        open = false
                    }
                ) { Text(if (allowClear) t["common_delete"] else t["close"]) }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

/**
 * 時刻を選ぶ欄。24 時間表記の HH:MM を返す。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(
    label: String,
    value: String?,
    onChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    allowClear: Boolean = true
) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    var open by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { open = true },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = value ?: label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (value == null) colors.textTertiary else colors.text
        )
    }
    if (open) {
        val parts = value?.split(':')
        val state = rememberTimePickerState(
            initialHour = parts?.getOrNull(0)?.toIntOrNull() ?: 23,
            initialMinute = parts?.getOrNull(1)?.toIntOrNull() ?: 59,
            is24Hour = true
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { open = false },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val hour = state.hour.toString().padStart(2, '0')
                        val minute = state.minute.toString().padStart(2, '0')
                        onChange("$hour:$minute")
                        open = false
                    }
                ) { Text(t["common_save"]) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (allowClear) onChange(null)
                        open = false
                    }
                ) { Text(if (allowClear) t["common_delete"] else t["close"]) }
            }
        )
    }
}

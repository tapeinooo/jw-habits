package app.jwhabits.ui.service

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.jwhabits.data.ServiceRecordEntity
import app.jwhabits.data.ServiceRole
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val zone: ZoneId get() = ZoneId.systemDefault()
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormat = DateTimeFormatter.ofPattern("yyyy年M月d日")

fun formatTime(ms: Long): String = Instant.ofEpochMilli(ms).atZone(zone).toLocalTime().format(timeFormat)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateButton(date: LocalDate, onChange: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier) { Text(date.format(dateFormat)) }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text("決定") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("やめる") } },
        ) { DatePicker(state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeButton(label: String, time: LocalTime, onChange: (LocalTime) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier) { Text("$label ${time.format(timeFormat)}") }
    if (open) {
        val state = rememberTimePickerState(time.hour, time.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = { onChange(LocalTime.of(state.hour, state.minute)); open = false }) { Text("決定") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("やめる") } },
        )
    }
}

private fun digits(s: String) = s.filter(Char::isDigit).take(4)

/** 手で時間を足す。 */
@Composable
fun ManualAddDialog(initialDate: LocalDate, onDismiss: () -> Unit, onAdd: (LocalDate, Int) -> Unit) {
    var date by remember { mutableStateOf(initialDate) }
    var hours by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    val total = (hours.toIntOrNull() ?: 0) * 60 + (minutes.toIntOrNull() ?: 0)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("手で入れる") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DateButton(date, { date = it }, Modifier.fillMaxWidth())
                DurationFields(hours, minutes, { hours = digits(it) }, { minutes = digits(it) })
            }
        },
        confirmButton = { TextButton(enabled = total > 0, onClick = { onAdd(date, total) }) { Text("追加") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("やめる") } },
    )
}

@Composable
private fun DurationFields(hours: String, minutes: String, onHours: (String) -> Unit, onMinutes: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            hours, onHours, label = { Text("時間") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            minutes, onMinutes, label = { Text("分") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 記録の直し。タイマーの記録は日付と開始・終了の時刻（終了し忘れもここで直す）、分数の記録は日付と分数。
 * 日付を変えたときは、開始・終了も同じ日数だけずらす。終了が開始以前なら翌日の終了とみなす。
 */
@Composable
fun EditRecordDialog(
    record: ServiceRecordEntity,
    onDismiss: () -> Unit,
    onSave: (ServiceRecordEntity) -> Unit,
    onDelete: () -> Unit,
) {
    var date by remember { mutableStateOf(record.date) }
    val isTimer = record.minutes == null && record.startEpochMs != null
    val startMs = record.startEpochMs
    var startTime by remember {
        mutableStateOf(startMs?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalTime() } ?: LocalTime.of(10, 0))
    }
    val running = isTimer && record.endEpochMs == null
    var endTime by remember {
        mutableStateOf(record.endEpochMs?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalTime() } ?: LocalTime.of(12, 0))
    }
    val m0 = record.minutes ?: 0
    var hours by remember { mutableStateOf(if (record.minutes != null) (m0 / 60).toString() else "") }
    var minutes by remember { mutableStateOf(if (record.minutes != null) (m0 % 60).toString() else "") }
    val total = (hours.toIntOrNull() ?: 0) * 60 + (minutes.toIntOrNull() ?: 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("記録を直す") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DateButton(date, { date = it }, Modifier.fillMaxWidth())
                if (isTimer) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimeButton("開始", startTime, { startTime = it }, Modifier.weight(1f))
                        if (!running) TimeButton("終了", endTime, { endTime = it }, Modifier.weight(1f))
                    }
                    if (running) Text("タイマー動作中", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    DurationFields(hours, minutes, { hours = digits(it) }, { minutes = digits(it) })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isTimer || total > 0,
                onClick = {
                    if (!isTimer) {
                        onSave(record.copy(date = date, minutes = total))
                    } else {
                        val shiftDays = java.time.temporal.ChronoUnit.DAYS.between(record.date, date)
                        val origStartDate = Instant.ofEpochMilli(startMs!!).atZone(zone).toLocalDate()
                        val newStart = origStartDate.plusDays(shiftDays).atTime(startTime).atZone(zone).toInstant().toEpochMilli()
                        val newEnd = if (running) null else {
                            var e = origStartDate.plusDays(shiftDays).atTime(endTime).atZone(zone).toInstant().toEpochMilli()
                            if (e <= newStart) {
                                e = origStartDate.plusDays(shiftDays + 1).atTime(endTime).atZone(zone).toInstant().toEpochMilli()
                            }
                            e
                        }
                        onSave(record.copy(date = date, startEpochMs = newStart, endEpochMs = newEnd))
                    }
                },
            ) { Text("保存") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("削除", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("やめる") }
            }
        },
    )
}

/** 奉仕の設定。基本の立場・目標時間・日付の切り替わり。 */
@Composable
fun ServiceSettingsDialog(
    role: ServiceRole,
    annualGoal: Int,
    monthlyGoal: Int,
    cutoffHour: Int,
    onDismiss: () -> Unit,
    onSave: (ServiceRole, Int, Int, Int) -> Unit,
) {
    var r by remember { mutableStateOf(role) }
    var annual by remember { mutableStateOf(annualGoal.toString()) }
    var monthly by remember { mutableStateOf(monthlyGoal.toString()) }
    var cutoff by remember { mutableStateOf(cutoffHour.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("奉仕の設定") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("基本の立場", style = MaterialTheme.typography.labelLarge)
                listOf(
                    ServiceRole.REGULAR_PIONEER to "正規開拓者",
                    ServiceRole.AUXILIARY_PIONEER to "補助開拓者",
                    ServiceRole.PUBLISHER to "伝道者",
                ).forEach { (value, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = r == value, onClick = { r = value })
                        Text(label)
                    }
                }
                NumberField("年間の目標（時間）", annual) { annual = digits(it) }
                NumberField("補助開拓の月の目標（時間）", monthly) { monthly = digits(it) }
                NumberField("日付が変わる時刻（0〜23時）", cutoff) { cutoff = digits(it) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    r, annual.toIntOrNull() ?: annualGoal, monthly.toIntOrNull() ?: monthlyGoal,
                    (cutoff.toIntOrNull() ?: cutoffHour).coerceIn(0, 23),
                )
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("やめる") } },
    )
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value, onChange, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

package app.jwhabits.ui.service

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.jwhabits.data.ServiceRecordEntity
import app.jwhabits.data.ServiceRole
import app.jwhabits.domain.ServiceMath
import app.jwhabits.ui.LocalUndoer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.text.font.FontWeight
import app.jwhabits.domain.CalendarMath
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ServiceScreen(vm: ServiceViewModel = viewModel(factory = ServiceViewModel.Factory)) {
    val undoer = LocalUndoer.current
    val context = LocalContext.current
    val s by vm.state.collectAsStateWithLifecycle()
    val baseRole by vm.baseRole.collectAsStateWithLifecycle()
    val annual by vm.annualGoal.collectAsStateWithLifecycle()
    val monthly by vm.monthlyGoal.collectAsStateWithLifecycle()
    val cutoff by vm.cutoffHour.collectAsStateWithLifecycle()
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    val dayTotals = remember(s.records, s.nowMs) { ServiceMath.dayTotals(s.records, s.nowMs) }
    var settings by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("奉仕", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = { settings = true }) { Icon(Icons.Filled.Settings, contentDescription = "設定") }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { vm.moveMonth(-1) }) { Text("‹", style = MaterialTheme.typography.titleLarge) }
                Text(
                    "${s.month.year}年${s.month.monthValue}月", style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { vm.moveMonth(1) }) { Text("›", style = MaterialTheme.typography.titleLarge) }
            }
        }
        item { SummaryCard(s) }
        item {
            ServiceCalendar(
                month = s.month, today = vm.today(), totals = dayTotals,
                onDay = { selectedDay = it },
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("聖書研究", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { vm.adjustStudies(-1) }, enabled = s.bibleStudies > 0) { Text("−") }
                    Text(
                        "${s.bibleStudies}件", style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    OutlinedButton(onClick = { vm.adjustStudies(1) }) { Text("+") }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("この月は補助開拓", modifier = Modifier.weight(1f))
                Switch(checked = s.auxiliaryThisMonth, onCheckedChange = { vm.setAuxiliary(it) })
            }
        }
        item {
            Button(
                onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("報告", s.reportText))
                    Toast.makeText(context, "コピーしました", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("報告をコピー") }
        }
    }

    selectedDay?.let { day ->
        DaySheet(
            date = day,
            records = s.records.filter { it.date == day },
            nowMs = s.nowMs,
            onAdjust = { vm.adjustDay(day, it) },
            onDelete = { r -> vm.delete(r, undoer::offer) },
            onDismiss = { selectedDay = null },
        )
    }
    if (settings) ServiceSettingsDialog(
        baseRole, annual, monthly, cutoff, onDismiss = { settings = false },
        onSave = { role, a, m, c -> settings = false; vm.saveSettings(role, a, m, c) },
    )
}

@Composable
private fun SummaryCard(s: ServiceState) {
    val sub = MaterialTheme.colorScheme.onSurfaceVariant
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (s.participated) "参加" else "まだ",
                style = if (s.role == ServiceRole.PUBLISHER) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleLarge,
                color = if (s.participated) MaterialTheme.colorScheme.primary else sub,
            )
            if (s.role != ServiceRole.PUBLISHER) {
                Text(ServiceMath.formatDuration(s.totalMinutes), style = MaterialTheme.typography.headlineMedium)
            }
            Text("研究 ${s.bibleStudies}件", color = sub)
            val first = s.month.atDay(1)
            when (s.role) {
                ServiceRole.REGULAR_PIONEER -> {
                    Text("${s.yearLabel} ${ServiceMath.formatDuration(s.yearTotalMinutes)} / ${s.annualGoal}時間", color = sub)
                    Text("年間の残り ${ServiceMath.formatDuration(ServiceMath.remainingMinutes(s.annualGoal, s.yearTotalMinutes))}", color = sub)
                    Text(
                        "月の目安 ${ServiceMath.monthlyGuideHours(s.annualGoal, s.yearTotalMinutes, first)}時間",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                ServiceRole.AUXILIARY_PIONEER -> {
                    Text("月の目標 ${s.monthlyGoal}時間", color = sub)
                    Text("残り ${ServiceMath.formatDuration(ServiceMath.remainingMinutes(s.monthlyGoal, s.totalMinutes))}", style = MaterialTheme.typography.titleMedium)
                }
                ServiceRole.PUBLISHER -> {}
            }
        }
    }
}

private val dayTitleFormat = DateTimeFormatter.ofPattern("M月d日（E）", Locale.JAPANESE)

@Composable
private fun ServiceCalendar(month: YearMonth, today: LocalDate, totals: Map<LocalDate, Long>, onDay: (LocalDate) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth()) {
                CalendarMath.weekdayLabels.forEach {
                    Text(
                        it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            CalendarMath.weeks(month).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        Box(Modifier.weight(1f).height(54.dp), contentAlignment = Alignment.Center) {
                            if (date != null) {
                                val isToday = date == today
                                val enabled = !date.isAfter(today)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable(enabled = enabled) { onDay(date) }.padding(2.dp),
                                ) {
                                    Box(
                                        Modifier.size(28.dp).then(
                                            if (isToday) Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape) else Modifier,
                                        ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "${date.dayOfMonth}", style = MaterialTheme.typography.bodyMedium,
                                            color = when {
                                                !enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                                isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                        )
                                    }
                                    val t = totals[date]
                                    Text(
                                        if (t != null) ServiceMath.formatCompact(t) else " ",
                                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary, maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySheet(
    date: LocalDate,
    records: List<ServiceRecordEntity>,
    nowMs: Long,
    onAdjust: (Int) -> Unit,
    onDelete: (ServiceRecordEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    val total = ServiceMath.totalMinutes(records, nowMs)
    val adjustable = records.filter { it.minutes != null && it.startEpochMs == null && it.endEpochMs == null }
        .maxByOrNull { it.id }
    val others = records.filter { it.id != adjustable?.id }
    // 少ない時間を左、多い時間を右に
    val steps = listOf(5 to "5分", 15 to "15分", 30 to "30分", 60 to "1時間")
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(date.format(dayTitleFormat), style = MaterialTheme.typography.titleMedium)
            Text(ServiceMath.formatDuration(total), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.forEach { (m, label) ->
                    Button(onClick = { onAdjust(m) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text("+$label", maxLines = 1)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.forEach { (m, label) ->
                    OutlinedButton(
                        onClick = { onAdjust(-m) }, enabled = (adjustable?.minutes ?: 0) > 0,
                        modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp),
                    ) { Text("−$label", maxLines = 1) }
                }
            }
            if (others.isNotEmpty()) {
                Column(Modifier.fillMaxWidth()) {
                    Text("ほかの記録", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    others.forEach { r ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            val what = if (r.minutes != null) "手入力" else
                                (r.startEpochMs?.let(::formatTime) ?: "") + "〜" + (r.endEpochMs?.let(::formatTime) ?: "")
                            Text(
                                "$what  ${ServiceMath.formatDuration(ServiceMath.durationMinutes(r, nowMs))}",
                                style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { onDelete(r) }) { Icon(Icons.Filled.Delete, contentDescription = "削除") }
                        }
                    }
                }
            }
            TextButton(onClick = onDismiss) { Text("閉じる") }
        }
    }
}

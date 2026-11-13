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

/** 経過時間 "1:05:09"。 */
fun formatElapsed(ms: Long): String {
    val s = maxOf(0L, ms) / 1000
    return "%d:%02d:%02d".format(s / 3600, s % 3600 / 60, s % 60)
}

/**
 * 「開始」「終了」のボタン（奉仕タブと今日の画面で共通）。動いている間は経過時間を出す。
 * 初めての開始で通知の許可を頼む（断られてもタイマーは動く）。
 */
@Composable
fun TimerButton(vm: ServiceViewModel, modifier: Modifier = Modifier) {
    val running by vm.running.collectAsStateWithLifecycle()
    val now by vm.nowTicker.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.startTimer() }
    val r = running
    if (r == null) {
        Button(
            onClick = {
                val needs = Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                if (needs) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.startTimer()
            },
            modifier = modifier.fillMaxWidth().height(56.dp),
        ) { Text("開始", style = MaterialTheme.typography.titleMedium) }
    } else {
        Button(onClick = { vm.stopTimer() }, modifier = modifier.fillMaxWidth().height(56.dp)) {
            Text("終了  ${formatElapsed(now - (r.startEpochMs ?: now))}", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun ServiceScreen(vm: ServiceViewModel = viewModel(factory = ServiceViewModel.Factory)) {
    val undoer = LocalUndoer.current
    val context = LocalContext.current
    val s by vm.state.collectAsStateWithLifecycle()
    val baseRole by vm.baseRole.collectAsStateWithLifecycle()
    val annual by vm.annualGoal.collectAsStateWithLifecycle()
    val monthly by vm.monthlyGoal.collectAsStateWithLifecycle()
    val cutoff by vm.cutoffHour.collectAsStateWithLifecycle()
    var manual by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ServiceRecordEntity?>(null) }

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
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimerButton(vm)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(15 to "+15分", 30 to "+30分", 60 to "+1時間").forEach { (min, label) ->
                            OutlinedButton(
                                onClick = { vm.quickAdd(min, label.removePrefix("+"), undoer::offer) },
                                modifier = Modifier.weight(1f),
                            ) { Text(label, maxLines = 1) }
                        }
                    }
                    OutlinedButton(onClick = { manual = true }, modifier = Modifier.fillMaxWidth()) { Text("手で入れる") }
                }
            }
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
        item { Text("記録", style = MaterialTheme.typography.titleMedium) }
        if (s.records.isEmpty()) {
            item { Text("この月の記録はまだありません。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(s.records, key = { it.id }) { r -> RecordRow(r, s.nowMs, onClick = { editing = r }) }
    }

    if (manual) ManualAddDialog(
        initialDate = vm.today(), onDismiss = { manual = false },
        onAdd = { date, min -> manual = false; vm.addManual(date, min, undoer::offer) },
    )
    editing?.let { r ->
        EditRecordDialog(
            r, onDismiss = { editing = null },
            onSave = { editing = null; vm.save(it) },
            onDelete = { editing = null; vm.delete(r, undoer::offer) },
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

@Composable
private fun RecordRow(r: ServiceRecordEntity, nowMs: Long, onClick: () -> Unit) {
    val what = if (r.minutes != null) "${r.minutes}分" else {
        val start = r.startEpochMs?.let(::formatTime) ?: ""
        "$start〜" + (r.endEpochMs?.let(::formatTime) ?: "")
    }
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${r.date.monthValue}/${r.date.dayOfMonth}", style = MaterialTheme.typography.titleSmall)
                Text(what, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (r.minutes == null && r.endEpochMs == null) "動作中 " + ServiceMath.formatDuration(ServiceMath.durationMinutes(r, nowMs))
                else ServiceMath.formatDuration(ServiceMath.durationMinutes(r, nowMs)),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

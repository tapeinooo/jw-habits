package app.jwhabits.ui.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.jwhabits.ui.LocalUndoer
import java.time.LocalDate

/** 設定の画面。Dialog ではなく、アプリの中の全画面。 */
@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val undoer = LocalUndoer.current
    val cutoff by vm.cutoffHour.collectAsStateWithLifecycle()
    val last by vm.lastBackupDate.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler(onBack = onBack)
    LaunchedEffect(vm) { vm.messages.collect { undoer.message(it) } }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) vm.exportTo(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) vm.readForImport(uri)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る") }
            Text("設定", style = MaterialTheme.typography.headlineSmall)
        }

        app.jwhabits.update.UpdateSection()
        HorizontalDivider()

        Text("日付の切り替わり", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("日付が変わる時刻")
            var open by remember { mutableStateOf(false) }
            OutlinedButton(onClick = { open = true }) { Text("${cutoff}時") }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                (0..23).forEach { h ->
                    DropdownMenuItem(text = { Text("${h}時") }, onClick = { vm.setCutoff(h); open = false })
                }
            }
        }

        HorizontalDivider()
        Text("バックアップ", style = MaterialTheme.typography.titleMedium)
        Text(
            "機種変更や再インストールの前に書き出して、ファイルを Google ドライブなどに保存してください。",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(enabled = !busy, onClick = { exportLauncher.launch(vm.defaultFileName(LocalDate.now())) }) { Text("書き出す") }
            OutlinedButton(enabled = !busy, onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) { Text("読み込む") }
        }
        Text(
            last?.let { "最後に書き出した日：$it" } ?: "まだ書き出していません",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )

        HorizontalDivider()
        Text("アプリの版", style = MaterialTheme.typography.titleMedium)
        Text(vm.versionName(context), style = MaterialTheme.typography.bodyMedium)
    }

    pending?.let { p ->
        AlertDialog(
            onDismissRequest = vm::cancelImport,
            title = { Text("読み込みの確認") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("今のデータをすべて、このファイルの内容に置き換えます。")
                    Text("書き出した日時：${p.exportedAt}")
                    Text("章の記録 ${p.unitRecords}件 / 資料 ${p.materials}件 / 奉仕の記録 ${p.serviceRecords}件")
                }
            },
            confirmButton = { TextButton(onClick = vm::confirmImport) { Text("置き換える") } },
            dismissButton = { TextButton(onClick = vm::cancelImport) { Text("やめる") } },
        )
    }
}

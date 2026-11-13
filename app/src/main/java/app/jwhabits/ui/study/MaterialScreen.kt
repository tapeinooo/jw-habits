package app.jwhabits.ui.study

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import app.jwhabits.data.MaterialTemplate
import app.jwhabits.ui.LocalUndoer
import app.jwhabits.ui.bible.BibleUnits
import app.jwhabits.ui.units.UnitGrid
import app.jwhabits.ui.units.UnitTopBar
import app.jwhabits.ui.units.UnitUiState
import app.jwhabits.ui.units.UnitViewModel
import app.jwhabits.ui.units.openLink
import app.jwhabits.ui.units.shortDate

/** 資料1つの画面。種類（聖書／数のある資料／記録だけの資料）で中身を切り替える。 */
@Composable
fun MaterialScreen(materialId: Long, study: StudyViewModel, onBack: () -> Unit) {
    val vm: UnitViewModel = viewModel(key = UnitViewModel.key(materialId), factory = UnitViewModel.factory(materialId))
    val state by vm.state.collectAsStateWithLifecycle()
    val currentId by study.currentId.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val material = state.material

    BackHandler(onBack = onBack)
    LaunchedEffect(material?.archived) { if (material?.archived == true) onBack() }
    if (!state.loaded || material == null) {
        Box(Modifier.fillMaxSize())
        return
    }

    var editing by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf(false) }
    val isCurrent = currentId == materialId

    val extras: @Composable ColumnScope.(() -> Unit) -> Unit = { close ->
        DropdownMenuItem(
            text = { Text(if (isCurrent) "今使っている資料をやめる" else "今使っている資料にする") },
            onClick = { close(); if (isCurrent) study.clearCurrent() else study.setCurrent(materialId) },
        )
        DropdownMenuItem(text = { Text("名前・数を変える") }, onClick = { close(); editing = true })
        if (material.template == MaterialTemplate.WATCHTOWER) {
            DropdownMenuItem(text = { Text("次の号を追加") }, onClick = {
                close()
                study.addNextIssue(material) { added ->
                    Toast.makeText(context, if (added != null) "${added.title} を追加しました" else "次の号はすでにあります", Toast.LENGTH_SHORT).show()
                }
            })
        }
        DropdownMenuItem(text = { Text("資料を外す") }, onClick = { close(); confirmArchive = true })
    }

    when {
        material.template == MaterialTemplate.BIBLE_STUDY -> BibleUnits(state, vm, material.title, onBack, extras)
        state.countless -> CountlessLog(state, vm, material.title, onBack, extras)
        else -> UnitGrid(state, vm, material.title, onBack, extras)
    }

    if (editing) {
        EditMaterialDialog(material, onDismiss = { editing = false }, onSave = { editing = false; study.update(it) })
    }
    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text("この資料を外しますか") },
            text = { Text("一覧から外れます。記録は残ります。") },
            confirmButton = { TextButton(onClick = { confirmArchive = false; study.archive(materialId) }) { Text("外す") } },
            dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("やめる") } },
        )
    }
}

/** 数を決めない資料：記録した日の一覧（新しい順）と「今日記録」。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CountlessLog(
    state: UnitUiState,
    vm: UnitViewModel,
    title: String,
    onBack: () -> Unit,
    menuExtras: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    val undoer = LocalUndoer.current
    val context = LocalContext.current
    val today = vm.today()
    Column(Modifier.fillMaxSize()) {
        UnitTopBar(title, "資料の一覧へ", onBack) { close -> menuExtras(close) }
        Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "${state.log.size}件 記録" + if (state.todayCount > 0) " ・ 今日 ${state.todayCount}件" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = { vm.addLog(undoer::offer) }, modifier = Modifier.fillMaxWidth()) { Text("今日記録") }
        }
        LazyColumn(Modifier.weight(1f).padding(top = 8.dp)) {
            items(state.log, key = { it.id }) { r ->
                Row(
                    Modifier.fillMaxWidth()
                        .combinedClickable(onClick = {}, onLongClick = { openLink(context, state.linkFor(r.unitIndex)) })
                        .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${r.unitIndex + 1}件目", Modifier.weight(1f))
                    Text(shortDate(r.date, today) + if (r.link != null) "  リンクあり" else "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClick = { vm.removeLog(r, undoer::offer) }) { Icon(Icons.Filled.Delete, "この記録を削除") }
                }
                HorizontalDivider()
            }
        }
    }
}

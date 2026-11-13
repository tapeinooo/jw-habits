package app.jwhabits.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.jwhabits.ui.LocalUndoer
import app.jwhabits.ui.units.NextRowFor
import app.jwhabits.ui.units.UnitViewModel

/** 「今日研究した」の大きいボタン。1回タップで付く／もう1回で外れる。 */
@Composable
fun StudiedTodayButton(done: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val content: @Composable () -> Unit = {
        if (done) {
            Icon(Icons.Filled.Check, contentDescription = null)
            Text("  今日研究した", style = MaterialTheme.typography.titleMedium)
        } else {
            Text("今日研究した", style = MaterialTheme.typography.titleMedium)
        }
    }
    if (done) {
        Button(onClick = onClick, modifier = modifier.fillMaxWidth().height(56.dp)) { content() }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth().height(56.dp)) { content() }
    }
}

/** 今使っている資料の「次」の行。選んでいなければ何も出さない。 */
@Composable
fun CurrentMaterialNext(currentId: Long, onOpen: ((Long) -> Unit)?, modifier: Modifier = Modifier) {
    if (currentId < 0) return
    val unitVm: UnitViewModel = viewModel(key = UnitViewModel.key(currentId), factory = UnitViewModel.factory(currentId))
    val state by unitVm.state.collectAsStateWithLifecycle()
    val material = state.material
    if (!state.loaded || material == null || material.archived) return
    NextRowFor(
        state, unitVm, modifier, showTitle = true,
        onClickLabel = onOpen?.let { open -> { _: Int -> open(currentId) } },
    )
}

private enum class AddStep { PICK, WATCHTOWER, MANUAL }

@Composable
fun StudyScreen(vm: StudyViewModel = viewModel(factory = StudyViewModel.Factory)) {
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }
    val id = openId
    if (id != null) {
        MaterialScreen(id, vm, onBack = { openId = null })
    } else {
        StudyHome(vm, onOpen = { openId = it })
    }
}

@Composable
private fun StudyHome(vm: StudyViewModel, onOpen: (Long) -> Unit) {
    val undoer = LocalUndoer.current
    val done by vm.studiedToday.collectAsStateWithLifecycle()
    val materials by vm.materials.collectAsStateWithLifecycle()
    val currentId by vm.currentId.collectAsStateWithLifecycle()
    var step by remember { mutableStateOf<AddStep?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { StudiedTodayButton(done, onClick = { vm.toggleToday(undoer::offer) }) }
            if (currentId >= 0) item { CurrentMaterialNext(currentId, onOpen = onOpen) }
            if (materials.isEmpty()) {
                item { Text("資料はまだありません。「資料を追加」から始められます。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(materials, key = { it.material.id }) { item ->
                MaterialCard(item, isCurrent = item.material.id == currentId, onClick = { onOpen(item.material.id) })
            }
        }
        ExtendedFloatingActionButton(
            onClick = { step = AddStep.PICK },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("資料を追加") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    when (step) {
        AddStep.PICK -> AddMaterialDialog(
            onDismiss = { step = null },
            onBibleStudy = { step = null; vm.create(app.jwhabits.domain.StudyTemplates.si()) },
            onBe = { step = null; vm.create(app.jwhabits.domain.StudyTemplates.be()) },
            onWatchtower = { step = AddStep.WATCHTOWER },
            onManual = { step = AddStep.MANUAL },
        )
        AddStep.WATCHTOWER -> WatchtowerIssueDialog(
            default = vm.today(), onDismiss = { step = null },
            onCreate = { y, m -> step = null; vm.create(app.jwhabits.domain.StudyTemplates.watchtower(y, m)) },
        )
        AddStep.MANUAL -> ManualMaterialDialog(
            onDismiss = { step = null },
            onCreate = { spec -> step = null; vm.create(spec) },
        )
        null -> {}
    }
}

@Composable
private fun MaterialCard(item: MaterialItem, isCurrent: Boolean, onClick: () -> Unit) {
    val m = item.material
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(m.title, style = MaterialTheme.typography.titleMedium)
                val progress = if (m.unitCount == null) "${item.done}件 記録" else
                    (if (m.round > 1) "${m.round}周目 " else "") + "${item.done}/${m.unitCount} ${m.unitLabel}"
                Text(progress, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isCurrent) Icon(Icons.Filled.Star, contentDescription = "今使っている資料", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

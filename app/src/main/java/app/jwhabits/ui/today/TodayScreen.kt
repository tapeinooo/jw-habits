package app.jwhabits.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.jwhabits.data.AppDatabase
import app.jwhabits.ui.LocalUndoer
import app.jwhabits.ui.study.CurrentMaterialNext
import app.jwhabits.ui.study.StudiedTodayButton
import app.jwhabits.ui.study.StudyViewModel
import app.jwhabits.ui.units.NextRowFor
import app.jwhabits.ui.units.UnitViewModel

/** 今日の画面。聖書通読と個人研究（奉仕・カレンダーは後の段階で足す）。 */
@Composable
fun TodayScreen(
    onOpenBible: () -> Unit,
    onOpenStudy: () -> Unit,
    bible: UnitViewModel = viewModel(
        key = UnitViewModel.key(AppDatabase.MATERIAL_BIBLE_READING),
        factory = UnitViewModel.factory(AppDatabase.MATERIAL_BIBLE_READING),
    ),
    study: StudyViewModel = viewModel(factory = StudyViewModel.Factory),
) {
    val undoer = LocalUndoer.current
    val state by bible.state.collectAsStateWithLifecycle()
    val studied by study.studiedToday.collectAsStateWithLifecycle()
    val currentId by study.currentId.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("今日", style = MaterialTheme.typography.headlineSmall)
        Card(onClick = onOpenBible, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("聖書通読", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (state.todayCount > 0) "今日 ${state.todayCount}章 読みました" else "今日はまだ記録がありません",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        NextRowFor(state, bible)
        Card(onClick = onOpenStudy, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("個人研究", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (studied) "今日は研究しました" else "今日はまだ記録がありません",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StudiedTodayButton(studied, onClick = { study.toggleToday(undoer::offer) })
            }
        }
        CurrentMaterialNext(currentId, onOpen = null)
    }
}

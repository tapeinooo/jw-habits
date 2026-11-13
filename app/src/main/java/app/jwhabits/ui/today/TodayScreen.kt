package app.jwhabits.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.jwhabits.ui.bible.BibleViewModel
import app.jwhabits.ui.bible.NextChapterRow

/** 今日の画面。今は聖書通読だけ（研究・奉仕・カレンダーは後の段階で足す）。 */
@Composable
fun TodayScreen(onOpenBible: () -> Unit, bible: BibleViewModel = viewModel(factory = BibleViewModel.Factory)) {
    val state by bible.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        NextChapterRow(state.next, bible, onOpenBook = null)
    }
}

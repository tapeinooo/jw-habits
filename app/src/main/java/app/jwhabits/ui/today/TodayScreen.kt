package app.jwhabits.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.jwhabits.data.AppDatabase
import app.jwhabits.domain.CalendarMath
import app.jwhabits.domain.ServiceMath
import app.jwhabits.ui.LocalUndoer
import app.jwhabits.ui.service.ServiceViewModel
import app.jwhabits.ui.study.CurrentMaterialNext
import app.jwhabits.ui.study.StudyViewModel
import app.jwhabits.ui.units.NextRowFor
import app.jwhabits.ui.units.UnitViewModel

@Composable
private fun DayMark.color(): Color = when (this) {
    DayMark.BIBLE -> MaterialTheme.colorScheme.primary
    DayMark.STUDY -> MaterialTheme.colorScheme.tertiary
    DayMark.SERVICE -> MaterialTheme.colorScheme.secondary
}

private fun DayMark.label(): String = when (this) {
    DayMark.BIBLE -> "通読"
    DayMark.STUDY -> "研究"
    DayMark.SERVICE -> "奉仕"
}

/** 今日の画面。状態3つ → 次の章・次の研究 → カレンダー → 連続日数（小さく）。 */
@Composable
fun TodayScreen(
    onOpenBible: () -> Unit,
    onOpenStudy: () -> Unit,
    onOpenSettings: () -> Unit = {},
    bible: UnitViewModel = viewModel(
        key = UnitViewModel.key(AppDatabase.MATERIAL_BIBLE_READING),
        factory = UnitViewModel.factory(AppDatabase.MATERIAL_BIBLE_READING),
    ),
    study: StudyViewModel = viewModel(factory = StudyViewModel.Factory),
    service: ServiceViewModel = viewModel(factory = ServiceViewModel.Factory),
    calendar: TodayViewModel = viewModel(factory = TodayViewModel.Factory),
) {
    val undoer = LocalUndoer.current
    val state by bible.state.collectAsStateWithLifecycle()
    val studied by study.studiedToday.collectAsStateWithLifecycle()
    val currentId by study.currentId.collectAsStateWithLifecycle()
    val monthMinutes by service.currentMonthMinutes.collectAsStateWithLifecycle()
    val cal by calendar.state.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("今日", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "設定") }
        }

        StatusRow(
            title = "聖書通読",
            status = if (state.todayCount > 0) "今日 ${state.todayCount}章" else "まだ",
            onClick = onOpenBible,
        )
        StatusRow(
            title = "個人研究",
            status = if (studied) "研究した" else "まだ",
            onClick = onOpenStudy,
        ) {
            if (studied) {
                Button(onClick = { study.toggleToday(undoer::offer) }, modifier = Modifier.height(44.dp)) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Text(" 研究した")
                }
            } else {
                OutlinedButton(onClick = { study.toggleToday(undoer::offer) }, modifier = Modifier.height(44.dp)) {
                    Text("今日研究した")
                }
            }
        }
        StatusRow(
            title = "奉仕",
            status = "今月 ${ServiceMath.formatDuration(monthMinutes)}",
            onClick = null,
        )

        NextRowFor(state, bible)
        CurrentMaterialNext(currentId, onOpen = null)

        CalendarCard(cal, onMove = calendar::moveMonth)

        val streaks = listOfNotNull(
            "通読 ${cal.bibleStreak}日連続".takeIf { cal.bibleStreak >= 2 },
            "研究 ${cal.studyStreak}日連続".takeIf { cal.studyStreak >= 2 },
        )
        streaks.forEach {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun StatusRow(
    title: String,
    status: String,
    onClick: (() -> Unit)?,
    action: (@Composable () -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            action?.invoke()
        }
    }
    if (onClick != null) Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) { content() }
    else Card(Modifier.fillMaxWidth()) { content() }
}

@Composable
private fun CalendarCard(cal: CalendarUiState, onMove: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onMove(-1) }) { Text("‹", style = MaterialTheme.typography.titleLarge) }
                Text(
                    "${cal.month.year}年${cal.month.monthValue}月",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onMove(1) }) { Text("›", style = MaterialTheme.typography.titleLarge) }
            }
            Row(Modifier.fillMaxWidth()) {
                CalendarMath.weekdayLabels.forEach {
                    Text(
                        it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            CalendarMath.weeks(cal.month).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        Box(Modifier.weight(1f).height(48.dp), contentAlignment = Alignment.Center) {
                            if (date != null) DayCell(date.dayOfMonth, date == cal.today, cal.marks[date].orEmpty())
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            ) {
                DayMark.entries.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(m.color(), CircleShape))
                        Text(" ${m.label()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, isToday: Boolean, marks: Set<DayMark>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(
            Modifier.size(28.dp).then(if (isToday) Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$day", style = MaterialTheme.typography.bodyMedium,
                color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(Modifier.height(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            DayMark.entries.filter { it in marks }.forEach { Box(Modifier.size(6.dp).background(it.color(), CircleShape)) }
        }
    }
}

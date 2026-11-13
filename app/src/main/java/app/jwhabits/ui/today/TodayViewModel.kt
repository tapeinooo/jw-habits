package app.jwhabits.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.jwhabits.JwHabitsApp
import app.jwhabits.data.CalendarRepository
import app.jwhabits.domain.Streak
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

/** カレンダーに出す記録の種類。 */
enum class DayMark { BIBLE, STUDY, SERVICE }

data class CalendarUiState(
    val month: YearMonth,
    val today: LocalDate,
    val marks: Map<LocalDate, Set<DayMark>> = emptyMap(),
    val bibleStreak: Int = 0,
    val studyStreak: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(private val repo: CalendarRepository, initialToday: LocalDate) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.from(initialToday))

    val state: StateFlow<CalendarUiState> = month.flatMapLatest { ym ->
        combine(repo.bibleDates, repo.studyDates, repo.serviceDates(ym), repo.todayFlow) { bible, study, service, today ->
            val marks = HashMap<LocalDate, MutableSet<DayMark>>()
            fun add(dates: Set<LocalDate>, mark: DayMark) =
                dates.filter { YearMonth.from(it) == ym }.forEach { marks.getOrPut(it) { linkedSetOf() }.add(mark) }
            add(bible, DayMark.BIBLE)
            add(study, DayMark.STUDY)
            add(service, DayMark.SERVICE)
            CalendarUiState(ym, today, marks, Streak.count(bible, today), Streak.count(study, today))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState(month.value, initialToday))

    val selectedMonth: StateFlow<YearMonth> = month

    fun moveMonth(delta: Int) { month.value = month.value.plusMonths(delta.toLong()) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as JwHabitsApp
                TodayViewModel(app.calendar, app.clock.today())
            }
        }
    }
}

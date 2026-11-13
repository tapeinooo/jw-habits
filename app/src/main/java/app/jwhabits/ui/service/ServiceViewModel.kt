package app.jwhabits.ui.service

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.jwhabits.JwHabitsApp
import app.jwhabits.data.ServicePrefs
import app.jwhabits.data.ServiceRecordEntity
import app.jwhabits.data.ServiceRepository
import app.jwhabits.data.ServiceRole
import app.jwhabits.domain.DayClock
import app.jwhabits.domain.ServiceMath
import app.jwhabits.ui.units.OfferUndo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** 表示している月のまとめ。 */
data class ServiceState(
    val month: YearMonth,
    val isCurrentMonth: Boolean,
    val records: List<ServiceRecordEntity> = emptyList(),
    val nowMs: Long = 0,
    val role: ServiceRole = ServiceRole.REGULAR_PIONEER,
    val auxiliaryThisMonth: Boolean = false,
    val bibleStudies: Int = 0,
    val totalMinutes: Long = 0,
    val yearTotalMinutes: Long = 0,
    val yearLabel: String = "",
    val annualGoal: Int = 0,
    val monthlyGoal: Int = 0,
) {
    val participated get() = ServiceMath.isParticipated(totalMinutes, bibleStudies)
    val reportText get() = ServiceMath.reportText(month, role, totalMinutes, bibleStudies)
}

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceViewModel(
    private val repo: ServiceRepository,
    private val prefs: ServicePrefs,
    private val clock: DayClock,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.from(repo.today()))
    val selectedMonth: StateFlow<YearMonth> = month

    val running: StateFlow<ServiceRecordEntity?> =
        repo.running.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 動いている間は毎秒、止まっていれば1回だけ流す現在時刻。 */
    private val now: Flow<Long> = repo.running.flatMapLatest { r ->
        if (r == null) flowOf(System.currentTimeMillis()) else flow {
            while (true) { emit(System.currentTimeMillis()); delay(1_000) }
        }
    }

    val state: StateFlow<ServiceState> = month.flatMapLatest { ym ->
        val monthRecords = repo.records(ym.atDay(1), ym.atEndOfMonth())
        val yearRange = ServiceMath.serviceYearRange(ym.atDay(1))
        val yearRecords = repo.records(yearRange.start, yearRange.endInclusive)
        val base = combine(monthRecords, yearRecords, repo.month(ym), now, repo.todayFlow) { m, y, row, nowMs, today ->
            val auxMonth = row?.role == ServiceRole.AUXILIARY_PIONEER
            ServiceState(
                month = ym,
                isCurrentMonth = ym == YearMonth.from(today),
                records = m,
                nowMs = nowMs,
                auxiliaryThisMonth = auxMonth,
                bibleStudies = row?.bibleStudies ?: 0,
                totalMinutes = ServiceMath.totalMinutes(m, nowMs),
                yearTotalMinutes = ServiceMath.totalMinutes(y, nowMs),
                yearLabel = ServiceMath.serviceYearLabel(ym.atDay(1)),
            )
        }
        combine(base, prefs.role, prefs.annualGoal, prefs.monthlyGoal) { s, baseRole, annual, monthly ->
            s.copy(
                role = if (s.auxiliaryThisMonth) ServiceRole.AUXILIARY_PIONEER else baseRole,
                annualGoal = annual, monthlyGoal = monthly,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ServiceState(month.value, true))

    /** 今月（今日の画面用）の合計。表示中の月とは別に持つ。 */
    val currentMonthMinutes: StateFlow<Long> = repo.todayFlow
        .map { YearMonth.from(it) }
        .distinctUntilChanged()
        .flatMapLatest { ym ->
            combine(repo.records(ym.atDay(1), ym.atEndOfMonth()), now) { r, n -> ServiceMath.totalMinutes(r, n) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val nowTicker: StateFlow<Long> = now.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), System.currentTimeMillis())

    val baseRole: StateFlow<ServiceRole> = prefs.role
    val annualGoal: StateFlow<Int> = prefs.annualGoal
    val monthlyGoal: StateFlow<Int> = prefs.monthlyGoal
    val cutoffHour: StateFlow<Int> = clock.cutoffFlow

    fun today(): LocalDate = repo.today()

    fun moveMonth(delta: Int) { month.value = month.value.plusMonths(delta.toLong()) }

    fun startTimer() = viewModelScope.launch { repo.start() }

    fun stopTimer() = viewModelScope.launch { repo.stop() }

    fun quickAdd(minutes: Int, label: String, onUndo: OfferUndo) = viewModelScope.launch {
        val id = repo.addMinutes(repo.today(), minutes)
        onUndo("$label を足しました") { repo.delete(id) }
    }

    fun addManual(date: LocalDate, minutes: Int, onUndo: OfferUndo) = viewModelScope.launch {
        val id = repo.addMinutes(date, minutes)
        onUndo("${ServiceMath.formatDuration(minutes.toLong())} を足しました") { repo.delete(id) }
    }

    fun save(r: ServiceRecordEntity) = viewModelScope.launch { repo.update(r) }

    fun delete(r: ServiceRecordEntity, onUndo: OfferUndo) = viewModelScope.launch {
        repo.delete(r.id)
        onUndo("記録を削除しました") { repo.restore(r) }
    }

    fun adjustStudies(delta: Int) = viewModelScope.launch {
        val cur = state.value
        repo.setBibleStudies(cur.month, (cur.bibleStudies + delta).coerceAtLeast(0))
    }

    fun setAuxiliary(on: Boolean) = viewModelScope.launch { repo.setAuxiliary(month.value, on) }

    fun saveSettings(role: ServiceRole, annual: Int, monthly: Int, cutoff: Int) {
        prefs.setRole(role)
        prefs.setAnnualGoal(annual)
        prefs.setMonthlyGoal(monthly)
        clock.cutoffHour = cutoff
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as JwHabitsApp
                ServiceViewModel(app.service, app.servicePrefs, app.clock)
            }
        }
    }
}

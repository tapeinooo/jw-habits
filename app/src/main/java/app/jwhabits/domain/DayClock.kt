package app.jwhabits.domain

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.LocalDateTime

/** 日付の切り替わり（既定は午前3時）。設定で変えられる。 */
class DayClock(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val cutoffState = MutableStateFlow(prefs.getInt(KEY_CUTOFF, DEFAULT_CUTOFF))

    val cutoffFlow: StateFlow<Int> = cutoffState

    var cutoffHour: Int
        get() = prefs.getInt(KEY_CUTOFF, DEFAULT_CUTOFF)
        set(value) {
            val v = value.coerceIn(0, 23)
            prefs.edit().putInt(KEY_CUTOFF, v).apply()
            cutoffState.value = v
        }

    fun today(now: LocalDateTime = LocalDateTime.now()): LocalDate = logicalDate(now, cutoffHour)

    /** 今日（日付の切り替わりを考えた日付）。変わるたびに流す。切り替えの時刻を設定で変えたときも流す。 */
    @OptIn(ExperimentalCoroutinesApi::class)
    val todayFlow: Flow<LocalDate> = cutoffState.flatMapLatest { cutoff ->
        flow {
            while (true) {
                emit(logicalDate(LocalDateTime.now(), cutoff))
                delay(TICK_MS)
            }
        }
    }.distinctUntilChanged()

    /** 次に日付が切り替わる時刻（端末の時計）。 */
    fun nextCutoff(now: LocalDateTime = LocalDateTime.now()): LocalDateTime = nextCutoff(now, cutoffHour)

    companion object {
        const val DEFAULT_CUTOFF = 3
        private const val TICK_MS = 30_000L

        fun nextCutoff(now: LocalDateTime, cutoffHour: Int): LocalDateTime {
            val todayCutoff = now.toLocalDate().atTime(cutoffHour, 0)
            return if (now.isBefore(todayCutoff)) todayCutoff else todayCutoff.plusDays(1)
        }
        private const val KEY_CUTOFF = "day_cutoff_hour"

        fun logicalDate(now: LocalDateTime, cutoffHour: Int): LocalDate =
            now.minusHours(cutoffHour.toLong()).toLocalDate()
    }
}

package app.jwhabits.domain

import android.content.Context
import java.time.LocalDate
import java.time.LocalDateTime

/** 日付の切り替わり（既定は午前3時）。設定で変えられる。 */
class DayClock(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var cutoffHour: Int
        get() = prefs.getInt(KEY_CUTOFF, DEFAULT_CUTOFF)
        set(value) = prefs.edit().putInt(KEY_CUTOFF, value.coerceIn(0, 23)).apply()

    fun today(now: LocalDateTime = LocalDateTime.now()): LocalDate = logicalDate(now, cutoffHour)

    companion object {
        const val DEFAULT_CUTOFF = 3
        private const val KEY_CUTOFF = "day_cutoff_hour"

        fun logicalDate(now: LocalDateTime, cutoffHour: Int): LocalDate =
            now.minusHours(cutoffHour.toLong()).toLocalDate()
    }
}

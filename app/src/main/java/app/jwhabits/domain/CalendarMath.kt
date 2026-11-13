package app.jwhabits.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** 月のカレンダーの計算（日曜はじまり）。画面に依らない純粋な関数。 */
object CalendarMath {
    /** 月の1日の前に空ける日数。日曜なら0、土曜なら6。 */
    fun leadingBlanks(month: YearMonth): Int = month.atDay(1).dayOfWeek.value % 7

    fun daysInMonth(month: YearMonth): Int = month.lengthOfMonth()

    /** 週ごとの行。空きは null、7日で1行（最後の週も null で埋める）。 */
    fun weeks(month: YearMonth): List<List<LocalDate?>> {
        val cells = List(leadingBlanks(month)) { null } + (1..daysInMonth(month)).map { month.atDay(it) }
        return cells.chunked(7).map { row -> row + List(7 - row.size) { null } }
    }

    val weekdayLabels = listOf("日", "月", "火", "水", "木", "金", "土")

    fun isSunday(d: LocalDate) = d.dayOfWeek == DayOfWeek.SUNDAY
}

/** 連続日数。 */
object Streak {
    /**
     * 今日から数えて、記録のある日が何日続いているか。
     * 今日にまだ記録がなければ昨日から数える（今日が終わるまでは途切れていない）。
     */
    fun count(dates: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in dates) today else today.minusDays(1)
        var n = 0
        while (day in dates) {
            n++
            day = day.minusDays(1)
        }
        return n
    }
}

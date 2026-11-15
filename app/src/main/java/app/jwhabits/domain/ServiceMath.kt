package app.jwhabits.domain

import app.jwhabits.data.ServiceRecordEntity
import app.jwhabits.data.ServiceRole
import java.time.LocalDate
import java.time.YearMonth

/** 奉仕時間の計算（画面や保存に依らない純粋な関数）。 */
object ServiceMath {
    private const val MS_PER_MINUTE = 60_000L

    /** 1件の分数。分数があればそれ、なければ 終了−開始（走行中は nowMs まで）。負にはしない。 */
    fun durationMinutes(r: ServiceRecordEntity, nowMs: Long): Long {
        r.minutes?.let { return maxOf(0L, it.toLong()) }
        val start = r.startEpochMs ?: return 0L
        val end = r.endEpochMs ?: nowMs
        return maxOf(0L, (end - start) / MS_PER_MINUTE)
    }

    fun totalMinutes(records: List<ServiceRecordEntity>, nowMs: Long): Long =
        records.sumOf { durationMinutes(it, nowMs) }

    fun isParticipated(totalMinutes: Long, bibleStudies: Int): Boolean = totalMinutes > 0 || bibleStudies > 0

    /** 奉仕年度の呼び名の年。9月〜翌8月で、終わる年（2026-09 → 2027）。 */
    fun serviceYear(date: LocalDate): Int = if (date.monthValue >= 9) date.year + 1 else date.year

    fun serviceYearRange(date: LocalDate): ClosedRange<LocalDate> {
        val y = serviceYear(date)
        return LocalDate.of(y - 1, 9, 1)..LocalDate.of(y, 8, 31)
    }

    fun serviceYearLabel(date: LocalDate): String = "${serviceYear(date)}奉仕年度"

    /** その月を含めて、年度の終わり（8月）までの月数。9月なら12、8月なら1。 */
    fun remainingMonths(date: LocalDate): Int = (8 - date.monthValue + 12) % 12 + 1

    /** 月の目安（時間）。年間の残り時間 ÷ 残りの月数、切り上げ。0以上。 */
    fun monthlyGuideHours(goalHours: Int, doneMinutes: Long, date: LocalDate): Int {
        val remainingMin = maxOf(0L, goalHours * 60L - doneMinutes)
        val perMonthMin = (remainingMin + remainingMonths(date) - 1) / remainingMonths(date)
        return ((perMonthMin + 59) / 60).toInt()
    }

    fun remainingMinutes(goalHours: Int, doneMinutes: Long): Long = maxOf(0L, goalHours * 60L - doneMinutes)

    /** 「52時間30分」。0分は「0分」、ちょうどは「52時間」。 */
    fun formatDuration(minutes: Long): String {
        val m = maxOf(0L, minutes)
        val h = m / 60
        val r = m % 60
        return when {
            h == 0L -> "${r}分"
            r == 0L -> "${h}時間"
            else -> "${h}時間${r}分"
        }
    }

    /** 報告のテキスト。時間は切り捨て（伝道者は時間なし）。 */
    fun reportText(month: YearMonth, role: ServiceRole, totalMinutes: Long, bibleStudies: Int): String {
        val m = "${month.monthValue}月"
        if (!isParticipated(totalMinutes, bibleStudies)) return "$m 不参加"
        val parts = mutableListOf("参加")
        if (role != ServiceRole.PUBLISHER) parts += "${totalMinutes / 60}時間"
        if (bibleStudies > 0) parts += "研究${bibleStudies}件"
        return "$m " + parts.joinToString("・")
    }

    /** 日ごとの加算後の分数。現在が無ければ0とみなし、0未満にはしない（0なら記録を消す）。 */
    fun applyDelta(currentMinutes: Int?, deltaMinutes: Int): Int =
        maxOf(0, (currentMinutes ?: 0) + deltaMinutes)

    /** 日付 → その日の合計分。0分の日は含めない。 */
    fun dayTotals(records: List<ServiceRecordEntity>, nowMs: Long): Map<LocalDate, Long> =
        records.groupBy { it.date }
            .mapValues { (_, rs) -> totalMinutes(rs, nowMs) }
            .filterValues { it > 0 }

    /** カレンダー用の「2:30」。 */
    fun formatCompact(minutes: Long): String {
        val m = maxOf(0L, minutes)
        return "%d:%02d".format(m / 60, m % 60)
    }
}

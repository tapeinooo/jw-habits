package app.jwhabits

import app.jwhabits.domain.CalendarMath
import app.jwhabits.domain.DayClock
import app.jwhabits.domain.Streak
import app.jwhabits.widget.WidgetUndoRules
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayLogicTest {
    @Test fun novemberStartsOnSunday() {
        val ym = YearMonth.of(2026, 11)
        assertEquals(0, CalendarMath.leadingBlanks(ym))
        assertEquals(30, CalendarMath.daysInMonth(ym))
        val weeks = CalendarMath.weeks(ym)
        assertEquals(5, weeks.size)
        assertEquals(LocalDate.of(2026, 11, 1), weeks[0][0])
        assertNull(weeks.last()[3]) // 30日は月曜、その後ろは空き
        assertEquals(LocalDate.of(2026, 11, 30), weeks.last()[1])
    }

    @Test fun februaryAndSaturdayStart() {
        // 2026-02-01 は日曜、28日まで
        assertEquals(0, CalendarMath.leadingBlanks(YearMonth.of(2026, 2)))
        assertEquals(28, CalendarMath.daysInMonth(YearMonth.of(2026, 2)))
        assertEquals(4, CalendarMath.weeks(YearMonth.of(2026, 2)).size)
        // 2026-08-01 は土曜 → 空き6
        assertEquals(6, CalendarMath.leadingBlanks(YearMonth.of(2026, 8)))
        // 2024-02 は閏年で29日、木曜はじまり → 空き4
        assertEquals(4, CalendarMath.leadingBlanks(YearMonth.of(2024, 2)))
        assertEquals(29, CalendarMath.daysInMonth(YearMonth.of(2024, 2)))
        CalendarMath.weeks(YearMonth.of(2024, 2)).forEach { assertEquals(7, it.size) }
    }

    private val today = LocalDate.of(2026, 11, 13)
    private fun days(vararg back: Int) = back.map { today.minusDays(it.toLong()) }.toSet()

    @Test fun streakCountsFromToday() {
        assertEquals(3, Streak.count(days(0, 1, 2), today))
    }

    @Test fun streakTodayMissingCountsFromYesterday() {
        assertEquals(2, Streak.count(days(1, 2), today))
    }

    @Test fun streakStopsAtGap() {
        assertEquals(2, Streak.count(days(0, 1, 3, 4), today))
        assertEquals(0, Streak.count(days(2, 3), today))
        assertEquals(0, Streak.count(emptySet(), today))
        assertEquals(1, Streak.count(days(0), today))
    }

    @Test fun nextCutoff() {
        assertEquals(LocalDateTime.of(2026, 11, 13, 3, 0), DayClock.nextCutoff(LocalDateTime.of(2026, 11, 13, 1, 0), 3))
        assertEquals(LocalDateTime.of(2026, 11, 14, 3, 0), DayClock.nextCutoff(LocalDateTime.of(2026, 11, 13, 3, 0), 3))
        assertEquals(LocalDateTime.of(2026, 11, 14, 3, 0), DayClock.nextCutoff(LocalDateTime.of(2026, 11, 13, 20, 0), 3))
    }

    @Test fun undoWindows() {
        assertTrue(WidgetUndoRules.shouldShow(1_000, 1_000))
        assertTrue(WidgetUndoRules.shouldShow(1_000, 5_999))
        assertFalse(WidgetUndoRules.shouldShow(1_000, 6_000))
        assertTrue(WidgetUndoRules.canUndo(1_000, 11_000))
        assertFalse(WidgetUndoRules.canUndo(1_000, 11_001))
        assertFalse(WidgetUndoRules.canUndo(5_000, 1_000)) // 時計が戻った
    }
}

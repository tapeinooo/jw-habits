package app.jwhabits

import app.jwhabits.data.ServiceRecordEntity
import app.jwhabits.data.ServiceRole
import app.jwhabits.domain.ServiceMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ServiceMathTest {
    private val d = LocalDate.of(2026, 11, 10)
    private fun timer(startMin: Long, endMin: Long?) =
        ServiceRecordEntity(date = d, startEpochMs = startMin * 60_000, endEpochMs = endMin?.let { it * 60_000 })

    @Test fun durationFromMinutesAndTimes() {
        assertEquals(30, ServiceMath.durationMinutes(ServiceRecordEntity(date = d, minutes = 30), 0))
        assertEquals(150, ServiceMath.durationMinutes(timer(600, 750), 0))
    }

    @Test fun durationNeverNegative() {
        assertEquals(0, ServiceMath.durationMinutes(timer(750, 600), 0))
        assertEquals(0, ServiceMath.durationMinutes(ServiceRecordEntity(date = d, minutes = -5), 0))
        assertEquals(0, ServiceMath.durationMinutes(timer(600, null), 500 * 60_000L))
    }

    @Test fun runningCountsUpToNow() {
        assertEquals(45, ServiceMath.durationMinutes(timer(600, null), 645 * 60_000L))
    }

    @Test fun monthTotal() {
        val rs = listOf(ServiceRecordEntity(date = d, minutes = 30), timer(0, 90), timer(100, null))
        assertEquals(30 + 90 + 10, ServiceMath.totalMinutes(rs, 110 * 60_000L))
        assertEquals(0, ServiceMath.totalMinutes(emptyList(), 0))
    }

    @Test fun serviceYearBoundaries() {
        val aug = LocalDate.of(2026, 8, 31)
        assertEquals(2026, ServiceMath.serviceYear(aug))
        assertEquals(LocalDate.of(2025, 9, 1)..LocalDate.of(2026, 8, 31), ServiceMath.serviceYearRange(aug))
        val sep = LocalDate.of(2026, 9, 1)
        assertEquals(2027, ServiceMath.serviceYear(sep))
        assertEquals(LocalDate.of(2026, 9, 1)..LocalDate.of(2027, 8, 31), ServiceMath.serviceYearRange(sep))
        assertEquals("2027奉仕年度", ServiceMath.serviceYearLabel(sep))
    }

    @Test fun monthlyGuide() {
        assertEquals(50, ServiceMath.monthlyGuideHours(600, 100 * 60L, LocalDate.of(2026, 11, 5)))
        assertEquals(0, ServiceMath.monthlyGuideHours(600, 700 * 60L, LocalDate.of(2026, 11, 5)))
        assertEquals(10, ServiceMath.monthlyGuideHours(600, 590 * 60L, LocalDate.of(2027, 8, 1)))
        // 切り上げ: 残り 501h ÷ 10 = 50.1 → 51
        assertEquals(51, ServiceMath.monthlyGuideHours(600, 99 * 60L, LocalDate.of(2026, 11, 5)))
        assertEquals(12, ServiceMath.remainingMonths(LocalDate.of(2026, 9, 1)))
        assertEquals(1, ServiceMath.remainingMonths(LocalDate.of(2027, 8, 31)))
    }

    @Test fun reportTexts() {
        val nov = YearMonth.of(2026, 11)
        assertEquals("11月 参加・52時間・研究2件", ServiceMath.reportText(nov, ServiceRole.REGULAR_PIONEER, 52 * 60 + 30, 2))
        assertEquals("11月 参加・30時間・研究1件", ServiceMath.reportText(nov, ServiceRole.AUXILIARY_PIONEER, 30 * 60, 1))
        assertEquals("11月 参加・研究2件", ServiceMath.reportText(nov, ServiceRole.PUBLISHER, 0, 2))
        assertEquals("11月 不参加", ServiceMath.reportText(nov, ServiceRole.REGULAR_PIONEER, 0, 0))
        assertEquals("11月 不参加", ServiceMath.reportText(nov, ServiceRole.PUBLISHER, 0, 0))
    }

    @Test fun reportFloorsHours() {
        val nov = YearMonth.of(2026, 11)
        assertEquals("11月 参加・52時間・研究2件", ServiceMath.reportText(nov, ServiceRole.REGULAR_PIONEER, 52 * 60 + 59, 2))
    }

    @Test fun participationAndFormat() {
        assertTrue(ServiceMath.isParticipated(1, 0))
        assertTrue(ServiceMath.isParticipated(0, 1))
        assertFalse(ServiceMath.isParticipated(0, 0))
        assertEquals("52時間30分", ServiceMath.formatDuration(52 * 60 + 30))
        assertEquals("52時間", ServiceMath.formatDuration(52 * 60))
        assertEquals("45分", ServiceMath.formatDuration(45))
    }
}

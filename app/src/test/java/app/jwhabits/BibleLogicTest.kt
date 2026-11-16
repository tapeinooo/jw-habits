package app.jwhabits

import app.jwhabits.domain.BibleBooks
import app.jwhabits.domain.DayClock
import app.jwhabits.domain.JwLibrary
import app.jwhabits.domain.ReadMark
import app.jwhabits.domain.ReadingProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BibleLogicTest {
    @Test fun has66BooksAnd1189Chapters() {
        assertEquals(66, BibleBooks.all.size)
        assertEquals(39, BibleBooks.all.count { it.isHebrew })
        assertEquals(1189, BibleBooks.totalChapters)
    }

    @Test fun chapterIndexRoundTrips() {
        for (i in 0 until BibleBooks.totalChapters) assertEquals(i, BibleBooks.chapterAt(i).index)
        assertEquals("創世記 1章", BibleBooks.chapterAt(0).label)
        assertEquals("啓示 22章", BibleBooks.chapterAt(1188).label)
        assertEquals("マタイ 1章", BibleBooks.chapterAt(BibleBooks.book(40).firstIndex).label)
    }

    @Test fun nextIsFirstWhenEmpty() = assertEquals(0, ReadingProgress.nextUnread(emptyList(), 10))

    @Test fun nextFollowsLastRecordedAndSkipsRead() {
        val marks = listOf(ReadMark(1, 5), ReadMark(2, 2), ReadMark(3, 3))
        assertEquals(4, ReadingProgress.nextUnread(marks, 10)) // 最後は3 → 4
        assertEquals(6, ReadingProgress.nextUnread(marks + ReadMark(4, 4), 10)) // 5 は既読
    }

    @Test fun nextWrapsAround() = assertEquals(0, ReadingProgress.nextUnread(listOf(ReadMark(1, 9)), 10))

    @Test fun nextNullWhenAllRead() = assertNull(ReadingProgress.nextUnread((0 until 10).map { ReadMark(it.toLong(), it) }, 10))

    @Test fun dayCutoffAt3am() {
        assertEquals(LocalDate.of(2026, 11, 12), DayClock.logicalDate(LocalDateTime.of(2026, 11, 13, 2, 59), 3))
        assertEquals(LocalDate.of(2026, 11, 13), DayClock.logicalDate(LocalDateTime.of(2026, 11, 13, 3, 0), 3))
    }

    @Test fun chapterUrlFormat() =
        assertEquals("https://www.jw.org/finder?wtlocale=J&bible=43003001", JwLibrary.chapterUrl(43, 3))

    @Test fun focusOpensBookInProgressOrCentersFinishedOne() {
        val gen = BibleBooks.book(1); val ex = BibleBooks.book(2)
        assertNull(app.jwhabits.domain.BibleFocus.of(emptyList()))
        // 創世記 1〜3章を読んだ → 創世記を開く
        val f1 = app.jwhabits.domain.BibleFocus.of((0..2).map { ReadMark(it.toLong(), it) })!!
        assertEquals(gen, f1.book); assertEquals(false, f1.finished)
        // 創世記を全部読んだ（最後が50章）→ 一覧で創世記を中央に
        val all = (0 until gen.chapters).map { ReadMark(it.toLong(), it) }
        val f2 = app.jwhabits.domain.BibleFocus.of(all)!!
        assertEquals(gen, f2.book); assertEquals(true, f2.finished)
        // その後に出エジプト記1章 → 出エジプト記を開く
        val f3 = app.jwhabits.domain.BibleFocus.of(all + ReadMark(999, ex.firstIndex))!!
        assertEquals(ex, f3.book); assertEquals(false, f3.finished)
    }
}

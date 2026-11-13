package app.jwhabits

import app.jwhabits.data.DayRecordEntity
import app.jwhabits.data.HabitEntity
import app.jwhabits.data.HabitKind
import app.jwhabits.data.MaterialEntity
import app.jwhabits.data.MaterialTemplate
import app.jwhabits.data.ServiceMonthEntity
import app.jwhabits.data.ServiceRecordEntity
import app.jwhabits.data.ServiceRole
import app.jwhabits.data.UnitRecordEntity
import app.jwhabits.data.backup.BackupCodec
import app.jwhabits.data.backup.BackupException
import app.jwhabits.data.backup.BackupFile
import app.jwhabits.data.backup.BackupSettings
import app.jwhabits.data.backup.toDto
import app.jwhabits.data.backup.toEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate

class BackupTest {
    private val d = LocalDate.of(2026, 11, 13)

    private fun sample() = BackupFile(
        dbVersion = 2,
        appVersion = "0.5.0",
        exportedAt = "2026-11-13T09:30:00+09:00",
        settings = BackupSettings(5, 7L, ServiceRole.AUXILIARY_PIONEER, 700, 50),
        habits = listOf(HabitEntity(1, "聖書通読", HabitKind.CHAPTER_GRID, 0).toDto()),
        materials = listOf(
            MaterialEntity(1, 1, "聖書通読", "章", 1189, template = MaterialTemplate.BIBLE_READING).toDto(),
            MaterialEntity(7, 1, "ものみの塔", "記事", null, link = "https://x", issue = "2026-11", round = 2, archived = true).toDto(),
        ),
        unitRecords = listOf(UnitRecordEntity(3, 1, 1, 0, d, "jwpub://x").toDto(), UnitRecordEntity(4, 1, 1, 1, d).toDto()),
        dayRecords = listOf(DayRecordEntity(1, d).toDto()),
        serviceRecords = listOf(
            ServiceRecordEntity(1, d, startEpochMs = 100L).toDto(),
            ServiceRecordEntity(2, d, minutes = 45).toDto(),
        ),
        serviceMonths = listOf(ServiceMonthEntity("2026-11", 2, ServiceRole.PUBLISHER).toDto()),
    )

    private fun failure(text: String): String =
        try { BackupCodec.decode(text); fail("should fail"); "" } catch (e: BackupException) { e.message ?: "" }

    @Test fun roundTrip() {
        val f = sample()
        assertEquals(f, BackupCodec.decode(BackupCodec.encode(f)))
    }

    @Test fun encodedShape() {
        val text = BackupCodec.encode(sample())
        assertTrue(text.contains("\"format\": \"jw-habits-backup\""))
        assertTrue(text.contains("\"date\": \"2026-11-13\""))
        assertTrue(text.contains("\"kind\": \"CHAPTER_GRID\""))
    }

    @Test fun rejectsWrongFormat() {
        val text = BackupCodec.encode(sample()).replace("jw-habits-backup", "other")
        assertTrue(failure(text).contains("バックアップではありません"))
        assertTrue(failure("not json").isNotEmpty())
    }

    @Test fun rejectsNewerSchema() {
        val text = BackupCodec.encode(sample()).replace("\"schemaVersion\": 1", "\"schemaVersion\": 2")
        assertTrue(failure(text).contains("新しい版"))
    }

    @Test fun rejectsMissingArray() {
        val text = BackupCodec.encode(sample()).replace("\"dayRecords\"", "\"dayRecordsX\"")
        assertTrue(failure(text).contains("dayRecords"))
    }

    @Test fun rejectsBadDate() {
        val text = BackupCodec.encode(sample()).replace("\"date\": \"2026-11-13\"", "\"date\": \"2026-13-45\"")
        assertTrue(failure(text).contains("日付"))
    }

    @Test fun ignoresUnknownKeys() {
        val text = BackupCodec.encode(sample()).replaceFirst("{", "{\"future\": [1, 2], ")
            .replace("\"title\": \"聖書通読\"", "\"title\": \"聖書通読\", \"extra\": true")
        assertEquals(sample(), BackupCodec.decode(text))
    }

    @Test fun mappingKeepsDatesEnumsAndNulls() {
        val m = MaterialEntity(7, 1, "t", "記事", null, issue = "2026-11")
        assertEquals(m, m.toDto().toEntity())
        assertNull(m.toDto().template)
        val u = UnitRecordEntity(3, 7, 2, 5, d, null)
        assertEquals("2026-11-13", u.toDto().date)
        assertEquals(u, u.toDto().toEntity())
        val s = ServiceRecordEntity(2, d, null, null, 45)
        assertEquals(s, s.toDto().toEntity())
        val mo = ServiceMonthEntity("2026-11", 3, ServiceRole.AUXILIARY_PIONEER)
        assertEquals(mo, mo.toDto().toEntity())
    }
}

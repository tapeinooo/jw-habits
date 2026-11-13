package app.jwhabits

import app.jwhabits.data.MaterialTemplate
import app.jwhabits.domain.ShareParser
import app.jwhabits.domain.StudyTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudyLogicTest {
    @Test fun parsesUrlAndTitleFromText() {
        val p = ShareParser.parse("ものみの塔 2026年11月号\nhttps://www.jw.org/finder?wtlocale=J&pub=w&issue=202611\n", null)!!
        assertEquals("https://www.jw.org/finder?wtlocale=J&pub=w&issue=202611", p.link)
        assertEquals("ものみの塔 2026年11月号", p.title)
        assertEquals("2026-11", p.issue)
    }

    @Test fun subjectIsPreferredAsTitle() {
        val p = ShareParser.parse("本文 https://example.org/a.", "  件名  ")!!
        assertEquals("件名", p.title)
        assertEquals("https://example.org/a", p.link)
        assertNull(p.issue)
    }

    @Test fun textWithoutUrl() {
        val p = ShareParser.parse("\n  ただのメモ \n二行目", null)!!
        assertNull(p.link)
        assertEquals("ただのメモ", p.title)
        assertNull(ShareParser.parse("  ", null))
    }

    @Test fun titleIsFirstNonBlankLineWithoutUrl() =
        assertEquals("題名", ShareParser.parse("https://a.example/x 題名\n次", null)!!.title)

    @Test fun nextIssueWrapsYear() {
        assertEquals(2027 to 1, StudyTemplates.nextIssue(2026, 12))
        assertEquals(2026 to 12, StudyTemplates.nextIssue(2026, 11))
    }

    @Test fun watchtowerTitleAndIssue() {
        assertEquals("ものみの塔（研究用） 2027年1月号", StudyTemplates.wtTitle(2027, 1))
        assertEquals("2027-01", StudyTemplates.issueKey(2027, 1))
        assertEquals(2027 to 1, StudyTemplates.parseIssue("2027-01"))
        assertNull(StudyTemplates.parseIssue("2027-13"))
    }

    @Test fun templateDefaults() {
        val wt = StudyTemplates.watchtower(2026, 11)
        assertEquals(4, wt.unitCount); assertEquals("記事", wt.unitLabel); assertEquals("2026-11", wt.issue)
        val be = StudyTemplates.be()
        assertEquals(53, be.unitCount); assertEquals("研究", be.unitLabel); assertEquals(MaterialTemplate.BE, be.template)
        val si = StudyTemplates.si()
        assertEquals(78, si.unitCount); assertEquals(MaterialTemplate.SI, si.template)
        val manual = StudyTemplates.manual("x")
        assertNull(manual.unitCount); assertEquals("章", manual.unitLabel); assertNull(manual.template)
    }

    @Test fun linkOrder() {
        assertEquals("rec", StudyTemplates.linkFor(MaterialTemplate.BE, "mat", null, 0, "rec"))
        assertEquals("mat", StudyTemplates.linkFor(MaterialTemplate.BE, "mat", null, 0, null))
        assertEquals(StudyTemplates.BE_URL, StudyTemplates.linkFor(MaterialTemplate.BE, null, null, 0, null))
        assertEquals("https://www.jw.org/finder?wtlocale=J&pub=w&issue=202611",
            StudyTemplates.linkFor(MaterialTemplate.WATCHTOWER, null, "2026-11", 1, null))
        assertEquals("https://www.jw.org/finder?wtlocale=J&bible=43003001",
            StudyTemplates.linkFor(MaterialTemplate.BIBLE_STUDY, null, null, 
                app.jwhabits.domain.BibleBooks.book(43).indexOf(3), null))
        assertNull(StudyTemplates.linkFor(null, null, null, 0, null))
    }

    @Test fun siHeadingsAndLinks() {
        assertEquals(78, app.jwhabits.domain.SiContents.count)
        assertEquals("聖書の1番目の書 ― 創世記", StudyTemplates.unitName(MaterialTemplate.SI, "項", 1))
        assertEquals("聖書の66番目の書 ― ヨハネへの啓示", StudyTemplates.unitName(MaterialTemplate.SI, "項", 66))
        assertEquals("研究10 ― 聖書 ― 信ぴょう性のある，真実の書", StudyTemplates.unitName(MaterialTemplate.SI, "項", 76))
        assertEquals("霊感を受けた聖書はとこしえの益をもたらす", StudyTemplates.unitName(MaterialTemplate.SI, "項", 77))
        assertEquals(app.jwhabits.domain.SiContents.TOC_URL, StudyTemplates.linkFor(MaterialTemplate.SI, null, null, null, null))
        // JW Library の共有リンクと同じ文書番号（創世記 = 1101990062、研究1 = 1101990128、結び = 1101990138）
        assertEquals("https://www.jw.org/finder?srcid=jwlshare&wtlocale=J&prefer=lang&docid=1101990062", StudyTemplates.linkFor(MaterialTemplate.SI, null, null, 1, null))
        assertEquals("https://www.jw.org/finder?srcid=jwlshare&wtlocale=J&prefer=lang&docid=1101990128", StudyTemplates.linkFor(MaterialTemplate.SI, null, null, 67, null))
        assertEquals("https://www.jw.org/finder?srcid=jwlshare&wtlocale=J&prefer=lang&docid=1101990138", StudyTemplates.linkFor(MaterialTemplate.SI, null, null, 77, null))
    }
}

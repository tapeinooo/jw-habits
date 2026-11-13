package app.jwhabits.domain

import app.jwhabits.data.MaterialTemplate

/** 資料のひな形（新しく作るときの既定値）と、リンク・題名の決まり。 */
object StudyTemplates {
    data class Spec(
        val title: String,
        val unitLabel: String,
        val unitCount: Int?,
        val template: MaterialTemplate?,
        val issue: String? = null,
    )

    const val BE_URL = "https://wol.jw.org/ja/wol/publication/r7/lp-j/be"
    const val WT_DEFAULT_COUNT = 4
    const val BE_COUNT = 53

    /** 『聖書全体は神の霊感を受けたもので，有益です』。見出しごとに1行。 */
    fun si() = Spec(SiContents.TITLE, "項", SiContents.count, MaterialTemplate.SI)

    fun be() = Spec("『神権宣教学校の教育から益を得る』", "研究", BE_COUNT, MaterialTemplate.BE)

    fun watchtower(year: Int, month: Int, unitCount: Int = WT_DEFAULT_COUNT) =
        Spec(wtTitle(year, month), "記事", unitCount, MaterialTemplate.WATCHTOWER, issueKey(year, month))

    fun manual(title: String, unitLabel: String = "章", unitCount: Int? = null) =
        Spec(title, unitLabel, unitCount, null)

    fun wtTitle(year: Int, month: Int) = "ものみの塔（研究用） ${year}年${month}月号"

    fun issueKey(year: Int, month: Int) = "%04d-%02d".format(year, month)

    /** "2026-11" → (2026, 11)。形が違えば null。 */
    fun parseIssue(issue: String?): Pair<Int, Int>? {
        val m = Regex("""(\d{4})-(\d{2})""").matchEntire(issue.orEmpty()) ?: return null
        val month = m.groupValues[2].toInt()
        return if (month in 1..12) m.groupValues[1].toInt() to month else null
    }

    fun nextIssue(year: Int, month: Int): Pair<Int, Int> = if (month >= 12) (year + 1) to 1 else year to (month + 1)

    /** 単位の表示名。聖書は「創世記 1章」、それ以外は「研究 12」。index は0始まり。 */
    fun unitName(template: MaterialTemplate?, unitLabel: String, index: Int): String = when (template) {
        MaterialTemplate.BIBLE_READING, MaterialTemplate.BIBLE_STUDY -> BibleBooks.chapterAt(index).label
        MaterialTemplate.SI -> SiContents.heading(index)
        else -> "$unitLabel ${index + 1}"
    }

    /** 長押しで開くリンク。単位の記録 → 資料 → ひな形の既定、の順で最初にあるもの。 */
    fun linkFor(
        template: MaterialTemplate?,
        materialLink: String?,
        issue: String?,
        unitIndex: Int?,
        recordLink: String?,
    ): String? = recordLink ?: materialLink ?: when (template) {
        MaterialTemplate.BIBLE_READING, MaterialTemplate.BIBLE_STUDY ->
            unitIndex?.takeIf { it in 0 until BibleBooks.totalChapters }?.let { JwLibrary.chapterUrl(BibleBooks.chapterAt(it)) }
        MaterialTemplate.SI -> unitIndex?.let(SiContents::url)
        MaterialTemplate.BE -> BE_URL
        MaterialTemplate.WATCHTOWER -> parseIssue(issue)?.let { (y, m) ->
            "https://www.jw.org/finder?wtlocale=J&pub=w&issue=%04d%02d".format(y, m)
        }
        null -> null
    }
}

package app.jwhabits.domain

/**
 * 聖書66書の書名と章数（アプリに固定で持つ。本文は持たない）。
 * 並びは JW Library と同じ。書番号は 1〜66（JW Library のリンクで使う）。
 * 書名は新世界訳の表記を記憶から入れている。違っていればここだけ直す。
 */
data class BibleBook(
    val number: Int,
    val name: String,
    val shortName: String,
    val chapters: Int,
    /** 全1189章を通した最初の章の番号（0始まり） */
    val firstIndex: Int,
) {
    val isHebrew: Boolean get() = number <= 39
    fun indexOf(chapter: Int): Int = firstIndex + chapter - 1
}

data class ChapterRef(val book: BibleBook, val chapter: Int) {
    val index: Int get() = book.indexOf(chapter)
    val label: String get() = "${book.name} ${chapter}章"
}

object BibleBooks {
    private val raw = listOf(
        "創世記" to "創" to 50, "出エジプト記" to "出" to 40, "レビ記" to "レビ" to 27,
        "民数記" to "民" to 36, "申命記" to "申" to 34, "ヨシュア記" to "ヨシュ" to 24,
        "裁き人の書" to "裁" to 21, "ルツ記" to "ルツ" to 4, "サムエル記第一" to "サ一" to 31,
        "サムエル記第二" to "サ二" to 24, "列王記第一" to "王一" to 22, "列王記第二" to "王二" to 25,
        "歴代誌第一" to "代一" to 29, "歴代誌第二" to "代二" to 36, "エズラ記" to "エズ" to 10,
        "ネヘミヤ記" to "ネヘ" to 13, "エステル記" to "エス" to 10, "ヨブ記" to "ヨブ" to 42,
        "詩編" to "詩" to 150, "格言の書" to "格" to 31, "伝道の書" to "伝" to 12,
        "ソロモンの歌" to "歌" to 8, "イザヤ書" to "イザ" to 66, "エレミヤ書" to "エレ" to 52,
        "哀歌" to "哀" to 5, "エゼキエル書" to "エゼ" to 48, "ダニエル書" to "ダニ" to 12,
        "ホセア書" to "ホセ" to 14, "ヨエル書" to "ヨエ" to 3, "アモス書" to "アモ" to 9,
        "オバデヤ書" to "オバ" to 1, "ヨナ書" to "ヨナ" to 4, "ミカ書" to "ミカ" to 7,
        "ナホム書" to "ナホ" to 3, "ハバクク書" to "ハバ" to 3, "ゼパニヤ書" to "ゼパ" to 3,
        "ハガイ書" to "ハガ" to 2, "ゼカリヤ書" to "ゼカ" to 14, "マラキ書" to "マラ" to 4,
        "マタイ" to "マタ" to 28, "マルコ" to "マル" to 16, "ルカ" to "ルカ" to 24,
        "ヨハネ" to "ヨハ" to 21, "使徒" to "使徒" to 28, "ローマ" to "ロマ" to 16,
        "コリント第一" to "コ一" to 16, "コリント第二" to "コ二" to 13, "ガラテア" to "ガラ" to 6,
        "エフェソス" to "エフ" to 6, "フィリピ" to "フィリ" to 4, "コロサイ" to "コロ" to 4,
        "テサロニケ第一" to "テ一" to 5, "テサロニケ第二" to "テ二" to 3, "テモテ第一" to "テモ一" to 6,
        "テモテ第二" to "テモ二" to 4, "テトス" to "テト" to 3, "フィレモン" to "フィレ" to 1,
        "ヘブライ" to "ヘブ" to 13, "ヤコブ" to "ヤコ" to 5, "ペテロ第一" to "ペ一" to 5,
        "ペテロ第二" to "ペ二" to 3, "ヨハネ第一" to "ヨ一" to 5, "ヨハネ第二" to "ヨ二" to 1,
        "ヨハネ第三" to "ヨ三" to 1, "ユダ" to "ユダ" to 1, "啓示" to "啓" to 22,
    )

    val all: List<BibleBook> = buildList {
        var first = 0
        raw.forEachIndexed { i, (names, chapters) ->
            add(BibleBook(i + 1, names.first, names.second, chapters, first))
            first += chapters
        }
    }

    val totalChapters: Int = all.sumOf { it.chapters }

    fun book(number: Int): BibleBook = all[number - 1]

    fun chapterAt(index: Int): ChapterRef {
        require(index in 0 until totalChapters) { "chapter index out of range: $index" }
        val book = all.last { it.firstIndex <= index }
        return ChapterRef(book, index - book.firstIndex + 1)
    }
}

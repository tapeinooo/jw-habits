package app.jwhabits.domain

/** 記録の一部（次の章の計算に要るものだけ）。id が大きいほど後に記録した。 */
data class ReadMark(val id: Long, val unitIndex: Int)

object ReadingProgress {
    /**
     * 次に読む章：最後に記録した章の次の未読の章（末尾まで行けば先頭へ戻る）。
     * 記録が1つもなければ最初の章。全部読み終えていれば null。
     */
    fun nextUnread(marks: Collection<ReadMark>, total: Int): Int? {
        val read = marks.mapTo(HashSet()) { it.unitIndex }
        val start = marks.maxByOrNull { it.id }?.let { it.unitIndex + 1 } ?: 0
        return (0 until total).asSequence().map { (start + it) % total }.firstOrNull { it !in read }
    }

    fun range(a: Int, b: Int): IntRange = if (a <= b) a..b else b..a
}

/** 聖書通読のタブを開いたときに見せる書。 */
object BibleFocus {
    /** book = 最後に記録した章の書。finished = その書を（今の周で）読み終えている。 */
    data class Focus(val book: BibleBook, val finished: Boolean)

    /** 記録がなければ null。 */
    fun of(marks: Collection<ReadMark>): Focus? {
        val last = marks.maxByOrNull { it.id } ?: return null
        if (last.unitIndex !in 0 until BibleBooks.totalChapters) return null
        val book = BibleBooks.chapterAt(last.unitIndex).book
        val read = marks.mapTo(HashSet()) { it.unitIndex }
        val finished = (book.firstIndex until book.firstIndex + book.chapters).all { it in read }
        return Focus(book, finished)
    }
}

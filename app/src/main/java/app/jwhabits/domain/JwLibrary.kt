package app.jwhabits.domain

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * JW Library を開くリンク。開き方は公式に文書化されていないため、
 * 実機で確かめた結果に合わせてここだけ直す（仕様書「実機で確かめること」）。
 */
object JwLibrary {
    /** 書2桁・章3桁・節3桁。JW Library が入っていればアプリで、なければブラウザで開く想定。 */
    fun chapterUrl(book: Int, chapter: Int): String =
        "https://www.jw.org/finder?wtlocale=J&bible=%02d%03d001".format(book, chapter)

    fun chapterUrl(ref: ChapterRef): String = chapterUrl(ref.book.number, ref.chapter)

    fun open(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "開けるアプリがありません", Toast.LENGTH_SHORT).show()
        }
    }
}

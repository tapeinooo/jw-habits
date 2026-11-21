package app.jwhabits.widget

import android.content.Context

/** ウィジェットの各タイルの背景の濃さ（0〜100%）。 */
object WidgetStyle {
    enum class Tile(val key: String, val default: Int) { BIBLE("bible", 100), STUDY("study", 100), SERVICE("service", 0) }

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences("widget_style", Context.MODE_PRIVATE)

    fun get(c: Context, t: Tile): Int = prefs(c).getInt(t.key, t.default).coerceIn(0, 100)

    fun set(c: Context, t: Tile, percent: Int) {
        prefs(c).edit().putInt(t.key, percent.coerceIn(0, 100)).apply()
    }

    /** 文字の大きさ（%）。100% = 端末のフォント倍率が標準のときと同じ大きさ。 */
    const val TEXT_MIN = 60
    const val TEXT_MAX = 160

    fun textScale(c: Context): Int = prefs(c).getInt("text_scale", 100).coerceIn(TEXT_MIN, TEXT_MAX)

    fun setTextScale(c: Context, percent: Int) {
        prefs(c).edit().putInt("text_scale", percent.coerceIn(TEXT_MIN, TEXT_MAX)).apply()
    }
}

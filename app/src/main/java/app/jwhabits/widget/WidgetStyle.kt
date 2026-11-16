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
}

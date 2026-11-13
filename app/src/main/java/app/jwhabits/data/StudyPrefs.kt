package app.jwhabits.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 「今使っている資料」。端末の設定に1つだけ持つ（-1 は未選択）。 */
class StudyPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _current = MutableStateFlow(prefs.getLong(KEY, NONE))
    val current: StateFlow<Long> = _current

    fun set(id: Long) {
        prefs.edit().putLong(KEY, id).apply()
        _current.value = id
    }

    fun clear() = set(NONE)

    companion object {
        const val NONE = -1L
        private const val KEY = "current_study_material"
    }
}

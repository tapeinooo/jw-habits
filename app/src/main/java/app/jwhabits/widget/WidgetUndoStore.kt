package app.jwhabits.widget

import android.content.Context
import java.time.LocalDate

/** ウィジェットで最後に記録したもの。「元に戻す」で、足した分だけを消す。 */
data class WidgetUndo(
    /** "bible" または "study" */
    val kind: String,
    val materialId: Long,
    /** 足した単位の記録の id */
    val unitRecordIds: List<Long>,
    /** 「今日研究した」を今回足したときだけ、その日付 */
    val studyDate: LocalDate?,
    val timestampMs: Long,
)

/** 時間の決まり（端末に依らない）。 */
object WidgetUndoRules {
    /** 「記録しました ・ 元に戻す」を出している時間 */
    const val SHOW_MS = 5_000L

    /** 元に戻せる時間 */
    const val UNDO_MS = 10_000L

    fun shouldShow(timestampMs: Long, nowMs: Long): Boolean = nowMs - timestampMs in 0 until SHOW_MS

    fun canUndo(timestampMs: Long, nowMs: Long): Boolean = nowMs - timestampMs in 0..UNDO_MS
}

class WidgetUndoStore(context: Context) {
    private val prefs = context.getSharedPreferences("widget_undo", Context.MODE_PRIVATE)

    fun save(u: WidgetUndo) {
        prefs.edit()
            .putString(KIND, u.kind)
            .putLong(MATERIAL, u.materialId)
            .putString(IDS, u.unitRecordIds.joinToString(","))
            .putString(STUDY_DATE, u.studyDate?.toString())
            .putLong(TIME, u.timestampMs)
            .apply()
    }

    fun load(): WidgetUndo? {
        val kind = prefs.getString(KIND, null) ?: return null
        return WidgetUndo(
            kind = kind,
            materialId = prefs.getLong(MATERIAL, -1),
            unitRecordIds = prefs.getString(IDS, "").orEmpty().split(",").mapNotNull { it.toLongOrNull() },
            studyDate = prefs.getString(STUDY_DATE, null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            timestampMs = prefs.getLong(TIME, 0),
        )
    }

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KIND = "kind"
        const val MATERIAL = "material_id"
        const val IDS = "unit_record_ids"
        const val STUDY_DATE = "study_date"
        const val TIME = "timestamp"
    }
}

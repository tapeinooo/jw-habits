package app.jwhabits.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.ActionCallback
import app.jwhabits.MainActivity
import app.jwhabits.data.AppDatabase
import kotlinx.coroutines.delay

/** ウィジェットから出す Intent（アプリを開く／リンクを開く）。 */
object WidgetIntents {
    const val EXTRA_TAB = "app.jwhabits.extra.TAB"

    /** tab は Tab の名前（"STUDY" など）。なければ今日の画面。 */
    fun openApp(context: Context, tab: String? = null): Action {
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (tab != null) intent.putExtra(EXTRA_TAB, tab)
        return actionStartActivity(intent)
    }

    /** JW Library（なければブラウザ）でリンクを開く。リンクがなければアプリを開く。 */
    fun openLink(context: Context, link: String?): Action =
        if (link == null) openApp(context)
        else actionStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

object WidgetKeys {
    val MaterialId = ActionParameters.Key<Long>("materialId")
    val UnitIndex = ActionParameters.Key<Int>("unitIndex")

    fun record(materialId: Long, index: Int?) = actionParametersOf(MaterialId to materialId, UnitIndex to (index ?: -1))
}

/** ✓：次の単位を今日の記録にする。アプリの画面と同じ UnitRepository を通る。 */
class RecordNextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val app = WidgetData.app(context)
        val materialId = parameters[WidgetKeys.MaterialId] ?: return
        val expected = parameters[WidgetKeys.UnitIndex]?.takeIf { it >= 0 }
        val db = app.db
        val m = db.materialDao().get(materialId)?.takeUnless { it.archived } ?: return
        val units = db.unitRecordDao()
        val repo = app.unitRepo(materialId)
        val date = app.clock.today()
        val isBible = materialId == AppDatabase.MATERIAL_BIBLE_READING
        val hadStudyDay = isBible || db.dayRecordDao().exists(AppDatabase.HABIT_STUDY, date)

        val ids: List<Long>
        if (m.unitCount == null) {
            repo.addLog()
            val last = units.maxIndex(materialId, m.round)
            ids = listOfNotNull(last?.let { units.get(materialId, m.round, it)?.id })
        } else {
            val idx = expected ?: return
            // 画面とずれていたり二度押ししたときに、記録を取り消さない
            if (units.get(materialId, m.round, idx) != null) return
            repo.toggle(idx)
            ids = listOfNotNull(units.get(materialId, m.round, idx)?.id)
        }
        if (ids.isEmpty()) return

        val now = System.currentTimeMillis()
        WidgetUndoStore(context).save(
            WidgetUndo(
                kind = if (isBible) "bible" else "study",
                materialId = materialId,
                unitRecordIds = ids,
                studyDate = if (!hadStudyDay && db.dayRecordDao().exists(AppDatabase.HABIT_STUDY, date)) date else null,
                timestampMs = now,
            )
        )
        WidgetUpdater.updateAll(context)
        // 「元に戻す」の表示は数秒だけ。過ぎたら描き直して消す。
        delay(WidgetUndoRules.SHOW_MS + 200)
        WidgetUpdater.updateAll(context)
    }
}

/** 元に戻す：直前にウィジェットで足した分だけを消す。10秒より古ければ何もしない。 */
class UndoAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val store = WidgetUndoStore(context)
        val e = store.load() ?: return
        store.clear()
        if (WidgetUndoRules.canUndo(e.timestampMs, System.currentTimeMillis())) {
            val db = WidgetData.app(context).db
            db.unitRecordDao().deleteByIds(e.unitRecordIds)
            e.studyDate?.let { db.dayRecordDao().delete(AppDatabase.HABIT_STUDY, it) }
        }
        WidgetUpdater.updateAll(context)
    }
}

class TimerStartAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetData.app(context).service.start()
        WidgetUpdater.updateAll(context)
    }
}

class TimerStopAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetData.app(context).service.stop()
        WidgetUpdater.updateAll(context)
    }
}

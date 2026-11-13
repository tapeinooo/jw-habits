package app.jwhabits.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import app.jwhabits.data.AppDatabase

/** 「次の章」ウィジェット（4×1）。✓ はアプリを開かずに今日の記録にする。 */
class NextChapterWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = WidgetData.app(context)
        suspend fun load() = WidgetData.bibleNext(app) to WidgetUndoStore(context).load()
            ?.takeIf { it.materialId == AppDatabase.MATERIAL_BIBLE_READING && WidgetUndoRules.shouldShow(it.timestampMs, System.currentTimeMillis()) }
        val initial = load()
        provideContent {
            val (next, undo) = rememberWidgetData(initial) { load() }
            WidgetFrame {
                when {
                    undo != null -> UndoLine()
                    next == null || next.index == null -> Column(
                        modifier = GlanceModifier.defaultWeight().clickable(WidgetIntents.openApp(context)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WText(if (next == null) "聖書通読が見つかりません" else "全章を読み終えました", size = 16, bold = true)
                    }
                    else -> NextUnitWidgetRow(
                        caption = "次の章",
                        label = next.label,
                        onClickLabel = WidgetIntents.openLink(context, next.link),
                        onCheck = actionRunCallback<RecordNextAction>(
                            WidgetKeys.record(AppDatabase.MATERIAL_BIBLE_READING, next.index)
                        ),
                    )
                }
            }
        }
    }
}

class NextChapterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextChapterWidget()
}

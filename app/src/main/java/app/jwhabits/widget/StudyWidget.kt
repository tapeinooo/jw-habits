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
import app.jwhabits.ui.Tab

/** 「研究」ウィジェット（4×1）。今使っている資料の次の単位。✓ で記録（「今日研究した」も入る）。 */
class StudyWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = WidgetData.app(context)
        suspend fun load(): Pair<WidgetData.Next?, WidgetUndo?> {
            val next = WidgetData.studyNext(app)
            val undo = WidgetUndoStore(context).load()
                ?.takeIf {
                    it.kind == "study" && it.materialId == next?.material?.id &&
                        WidgetUndoRules.shouldShow(it.timestampMs, System.currentTimeMillis())
                }
            return next to undo
        }
        val initial = load()
        provideContent {
            val (next, undo) = rememberWidgetData(initial) { load() }
            WidgetFrame {
                when {
                    next == null -> Column(
                        modifier = GlanceModifier.defaultWeight().clickable(WidgetIntents.openApp(context, Tab.STUDY.name)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) { WText("今使っている資料を選んでください", size = 15, bold = true, maxLines = 2) }
                    undo != null -> UndoLine()
                    next.allDone -> Column(
                        modifier = GlanceModifier.defaultWeight().clickable(WidgetIntents.openApp(context, Tab.STUDY.name)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WText(next.material.title, size = 11, muted = true)
                        WText("全て終えました", size = 18, bold = true)
                    }
                    else -> NextUnitWidgetRow(
                        caption = next.material.title,
                        label = next.label,
                        onClickLabel = WidgetIntents.openLink(context, next.link),
                        onCheck = actionRunCallback<RecordNextAction>(WidgetKeys.record(next.material.id, next.index)),
                    )
                }
            }
        }
    }
}

class StudyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StudyWidget()
}

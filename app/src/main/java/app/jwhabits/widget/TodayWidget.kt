package app.jwhabits.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column

/** 「今日」ウィジェット（2×1）。通読・研究・奉仕を、今日済んでいれば ✓、まだなら ・。タップでアプリを開く。 */
class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = WidgetData.app(context)
        val initial = WidgetData.todayMarks(app)
        provideContent {
            val marks = rememberWidgetData(initial) { WidgetData.todayMarks(app) }
            WidgetFrame(GlanceModifier.clickable(WidgetIntents.openApp(context))) {
                listOf("通読" to marks.bible, "研究" to marks.study, "奉仕" to marks.service).forEach { (label, done) ->
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        WText(label, size = 12, muted = true)
                        WText(
                            if (done) "✓" else "・", size = 24, bold = true,
                            color = if (done) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

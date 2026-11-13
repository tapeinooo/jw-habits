package app.jwhabits.widget

import android.content.Context
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.compose.ui.unit.dp
import androidx.glance.layout.height
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import app.jwhabits.R
import app.jwhabits.domain.ServiceMath
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 「奉仕タイマー」ウィジェット（2×1）。動いている間は開始時刻と、刻々進む経過時間。 */
class TimerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = WidgetData.app(context)
        val initial = WidgetData.timer(app)
        provideContent {
            val t = rememberWidgetData(initial) { WidgetData.timer(app) }
            TimerContent(context, t)
        }
    }
}

@androidx.compose.runtime.Composable
private fun TimerContent(context: Context, t: WidgetData.Timer) {
        val month = "今月 ${ServiceMath.formatDuration(t.monthMinutes)}"
        val start = t.startMs
        val views = start?.let {
            RemoteViews(context.packageName, R.layout.widget_timer_chronometer).apply {
                val elapsed = maxOf(0L, System.currentTimeMillis() - it)
                setChronometer(R.id.timer_chronometer, SystemClock.elapsedRealtime() - elapsed, null, true)
            }
        }
        val startText = start?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("H:mm")) + "から"
        }
        run {
            WidgetFrame {
                Column(modifier = GlanceModifier.defaultWeight().clickable(WidgetIntents.openApp(context))) {
                    if (views != null) {
                        AndroidRemoteViews(views, modifier = GlanceModifier.height(24.dp))
                        WText(startText.orEmpty(), size = 11, muted = true)
                    }
                    WText(month, size = if (views != null) 11 else 15, bold = views == null, muted = views != null)
                }
                if (views != null) Button("終了", onClick = actionRunCallback<TimerStopAction>())
                else Button("開始", onClick = actionRunCallback<TimerStartAction>())
            }
        }
}

class TimerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimerWidget()
}

package app.jwhabits.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import app.jwhabits.JwHabitsApp
import app.jwhabits.domain.ServiceMath
import app.jwhabits.ui.Tab
import java.time.YearMonth

/**
 * 「今日の3つ」ウィジェット（1行）。
 * 左：聖書通読（今日やったか＋次の章）、中：個人研究（今日やったか＋今の資料の次）、右：今月の奉仕時間とレッスン。
 * 通読と研究は、まだのとき赤みのある背景で知らせる。タップでそれぞれのタブを開く。
 */
class DailyWidget : GlanceAppWidget() {
    data class Data(
        val bibleDone: Boolean,
        val bibleNext: String,
        val studyDone: Boolean,
        val studyNext: String,
        val serviceMinutes: Long,
        val lessons: Int,
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = WidgetData.app(context)
        val initial = load(app)
        provideContent {
            val d = rememberWidgetData(initial) { load(app) }
            GlanceTheme {
                Row(
                    modifier = GlanceModifier.fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(20.dp)
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cell(
                        title = "通読", status = if (d.bibleDone) "✓" else "まだ", alert = !d.bibleDone,
                        detail = d.bibleNext, onClick = WidgetIntents.openApp(context, Tab.BIBLE.name),
                    )
                    Spacer(GlanceModifier.width(6.dp))
                    Cell(
                        title = "研究", status = if (d.studyDone) "✓" else "まだ", alert = !d.studyDone,
                        detail = d.studyNext, onClick = WidgetIntents.openApp(context, Tab.STUDY.name),
                    )
                    Spacer(GlanceModifier.width(6.dp))
                    Cell(
                        title = "奉仕", status = ServiceMath.formatDuration(d.serviceMinutes), alert = false,
                        detail = "レッスン ${d.lessons}件", onClick = WidgetIntents.openApp(context, Tab.SERVICE.name),
                    )
                }
            }
        }
    }

    private suspend fun load(app: JwHabitsApp): Data {
        val marks = WidgetData.todayMarks(app)
        val bible = WidgetData.bibleNext(app)
        val study = WidgetData.studyNext(app)
        val ym = YearMonth.from(app.clock.today())
        val timer = WidgetData.timer(app)
        val lessons = app.db.serviceDao().getMonth(ym.toString())?.bibleStudies ?: 0
        return Data(
            bibleDone = marks.bible,
            bibleNext = when {
                bible == null -> ""
                bible.allDone -> "読み終えました"
                else -> "次 " + shortLabel(bible.label)
            },
            studyDone = marks.study,
            studyNext = when {
                study == null -> "資料を選ぶ"
                study.allDone -> "終えました"
                study.countless -> study.material.title
                else -> "次 " + shortLabel(study.label)
            },
            serviceMinutes = timer.monthMinutes,
            lessons = lessons,
        )
    }

    companion object {
        private val siBook = Regex("""^聖書の\d+番目の書 ― (.+)$""")

        /** 1行に収まるよう短くする（「聖書の3番目の書 ― レビ記」→「レビ記」）。 */
        fun shortLabel(label: String): String = siBook.matchEntire(label)?.groupValues?.get(1) ?: label
    }
}

@Composable
private fun RowScope.Cell(title: String, status: String, alert: Boolean, detail: String, onClick: Action) {
    val bg = if (alert) GlanceTheme.colors.errorContainer else GlanceTheme.colors.secondaryContainer
    val fg = if (alert) GlanceTheme.colors.onErrorContainer else GlanceTheme.colors.onSecondaryContainer
    Column(
        modifier = GlanceModifier.defaultWeight().fillMaxHeight()
            .background(bg).cornerRadius(14.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WText(title, size = 12, color = fg)
            Spacer(GlanceModifier.width(4.dp))
            WText(status, size = 14, bold = true, color = fg)
        }
        WText(detail, size = 12, color = fg)
    }
}

class DailyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyWidget()
}

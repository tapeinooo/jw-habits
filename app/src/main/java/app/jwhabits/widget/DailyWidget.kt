package app.jwhabits.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.layout.Box
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import app.jwhabits.ui.Tab
import java.time.YearMonth

/**
 * 「今日の3つ」ウィジェット（1行）。
 * 左：聖書通読の次の章（シルバー）、中：今使っている資料の次（紺）、右：今月の奉仕時間とレッスンの数（透明）。
 * ラベルは出さず中身だけ。通読と研究は、まだの日ははっきりした色、済んだら薄くして ✓。タップでそれぞれのタブを開く。
 */
class DailyWidget : GlanceAppWidget() {
    data class Data(
        val bibleDone: Boolean,
        val bibleBook: String,
        val bibleNumber: String,
        val studyDone: Boolean,
        val studyText: String,
        val studyNumber: String,
        val serviceMinutes: Long,
        val lessons: Int,
        val alphaBible: Float = 1f,
        val alphaStudy: Float = 1f,
        val alphaService: Float = 0f,
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = WidgetData.app(context)
        val initial = load(app)
        provideContent {
            val d = rememberWidgetData(initial) { load(app) }
            Row(
                modifier = GlanceModifier.fillMaxSize().padding(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Tile(
                    done = d.bibleDone, bg = SILVER, fg = SILVER_INK, alpha = d.alphaBible,
                    big = d.bibleNumber, small = d.bibleBook,
                    onClick = WidgetIntents.openApp(context, Tab.BIBLE.name),
                )
                Spacer(GlanceModifier.width(8.dp))
                Tile(
                    done = d.studyDone, bg = NAVY, fg = NAVY_INK, alpha = d.alphaStudy,
                    big = d.studyNumber, small = d.studyText,
                    onClick = WidgetIntents.openApp(context, Tab.STUDY.name),
                )
                Spacer(GlanceModifier.width(8.dp))
                Column(
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                        .background(ColorProvider(Color.Black.copy(alpha = d.alphaService))).cornerRadius(18.dp)
                        .padding(horizontal = 6.dp)
                        .clickable(WidgetIntents.openApp(context, Tab.SERVICE.name)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    T(hm(d.serviceMinutes), 30, WHITE, bold = true)
                    T("${d.lessons}件", 18, WHITE, bold = true)
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
        val (bBook, bNum) = when {
            bible == null -> "" to ""
            bible.allDone -> "完読" to "✓"
            else -> splitNumber(bible.label)
        }
        val (sText, sNum) = when {
            study == null -> "資料を選ぶ" to ""
            study.allDone -> "完了" to "✓"
            study.countless -> study.material.title to ""
            else -> splitNumber(shortLabel(study.label))
        }
        return Data(
            marks.bible, bBook, bNum, marks.study, sText, sNum, timer.monthMinutes, lessons,
            WidgetStyle.get(app, WidgetStyle.Tile.BIBLE) / 100f,
            WidgetStyle.get(app, WidgetStyle.Tile.STUDY) / 100f,
            WidgetStyle.get(app, WidgetStyle.Tile.SERVICE) / 100f,
        )
    }

    companion object {
        private val siBook = Regex("""^聖書の\d+番目の書 ― (.+)$""")
        private val trailingNumber = Regex("""^(.*?)\s*(\d+)章?$""")

        /** 1行に収まるよう短くする（「聖書の3番目の書 ― レビ記」→「レビ記」）。 */
        fun shortLabel(label: String): String = siBook.matchEntire(label)?.groupValues?.get(1) ?: label

        /** 「創世記 4章」→（創世記, 4）、「研究 12」→（研究, 12）。番号がなければ（全体, ""）。 */
        fun splitNumber(label: String): Pair<String, String> =
            trailingNumber.matchEntire(label)?.let { it.groupValues[1] to it.groupValues[2] } ?: (label to "")

        /** 52時間30分 → "52:30"。 */
        fun hm(minutes: Long): String = "%d:%02d".format(minutes / 60, minutes % 60)

        private val SILVER = Color(0xFFC9CED6)
        private val SILVER_INK = Color(0xFF17202B)
        private val NAVY = Color(0xFF1C2E4A)
        private val NAVY_INK = Color(0xFFFFFFFF)
        private val WHITE = Color(0xFFFFFFFF)
    }
}

@Composable
private fun T(text: String, size: Int, color: Color, bold: Boolean = false, maxLines: Int = 1) {
    Text(
        text, maxLines = maxLines,
        style = TextStyle(
            color = ColorProvider(color), fontSize = size.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
        ),
    )
}

/**
 * まだ：はっきりした色。済んだ：薄くして右上に ✓。
 * 番号があれば大きく、名前は上に小さく。番号がなければ名前を大きく。
 */
@Composable
private fun RowScope.Tile(done: Boolean, bg: Color, fg: Color, alpha: Float, big: String, small: String, onClick: Action) {
    val fill = bg.copy(alpha = if (done) alpha * 0.28f else alpha)
    val ink = if (done) Color.White.copy(alpha = 0.85f) else if (alpha < 0.4f) Color.White else fg
    Box(
        modifier = GlanceModifier.defaultWeight().fillMaxHeight()
            .background(ColorProvider(fill)).cornerRadius(18.dp)
            .clickable(onClick),
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (big.isNotEmpty()) {
                T(small, 16, ink, bold = true)
                T(big, 32, ink, bold = true)
            } else {
                T(small, 24, ink, bold = true, maxLines = 2)
            }
        }
        if (done) {
            Box(GlanceModifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.TopEnd) {
                T("✓", 22, Color.White, bold = true)
            }
        }
    }
}

class DailyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyWidget()
}

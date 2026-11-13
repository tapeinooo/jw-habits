package app.jwhabits.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.flow.MutableStateFlow

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll

object WidgetUpdater {
    /**
     * データが変わるたびに増える番号。Glance はセッションが続いている間 provideGlance をやり直さないので、
     * 各ウィジェットはこの番号を見て中身を読み直す（rememberWidgetData）。
     */
    val version = MutableStateFlow(0L)

    /** 4つのウィジェットをまとめて描き直す。 */
    suspend fun updateAll(context: Context) {
        version.value++
        val widgets: List<GlanceAppWidget> = listOf(DailyWidget(), TodayWidget(), NextChapterWidget(), StudyWidget(), TimerWidget())
        widgets.forEach { runCatching { it.updateAll(context) } }
    }
}

/** ウィジェットの中身。WidgetUpdater.version が変わるたびに load() で読み直す。 */
@Composable
fun <T> rememberWidgetData(initial: T, load: suspend () -> T): T {
    val v by WidgetUpdater.version.collectAsState()
    val data by produceState(initial, v) { value = load() }
    return data
}

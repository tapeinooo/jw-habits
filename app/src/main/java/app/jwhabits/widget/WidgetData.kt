package app.jwhabits.widget

import android.content.Context
import app.jwhabits.JwHabitsApp
import app.jwhabits.data.AppDatabase
import app.jwhabits.data.MaterialEntity
import app.jwhabits.data.StudyPrefs
import app.jwhabits.domain.ReadMark
import app.jwhabits.domain.ReadingProgress
import app.jwhabits.domain.ServiceMath
import app.jwhabits.domain.StudyTemplates
import java.time.YearMonth

/** ウィジェットが読むデータ（provideGlance の中で suspend の DAO を呼ぶ）。 */
object WidgetData {
    fun app(context: Context): JwHabitsApp = context.applicationContext as JwHabitsApp

    data class TodayMarks(val bible: Boolean, val study: Boolean, val service: Boolean)

    suspend fun todayMarks(app: JwHabitsApp): TodayMarks {
        val today = app.clock.today()
        val db = app.db
        val now = System.currentTimeMillis()
        return TodayMarks(
            bible = db.unitRecordDao().getCountOn(AppDatabase.MATERIAL_BIBLE_READING, today) > 0,
            study = db.dayRecordDao().exists(AppDatabase.HABIT_STUDY, today),
            service = db.serviceDao().getRange(today, today).any { ServiceMath.durationMinutes(it, now) > 0 },
        )
    }

    /** 資料の「次」。index は次の単位（数を決めない資料と、読み終えたときは null）。 */
    data class Next(
        val material: MaterialEntity,
        val index: Int?,
        val countless: Boolean,
        val allDone: Boolean,
        val label: String,
        val link: String?,
    )

    suspend fun next(app: JwHabitsApp, materialId: Long): Next? {
        val m = app.db.materialDao().get(materialId) ?: return null
        if (m.archived) return null
        val total = m.unitCount
        if (total == null) {
            return Next(m, null, countless = true, allDone = false, label = "今日記録",
                link = StudyTemplates.linkFor(m.template, m.link, m.issue, null, null))
        }
        val marks = app.db.unitRecordDao().getRound(materialId, m.round).map { ReadMark(it.id, it.unitIndex) }
        val idx = ReadingProgress.nextUnread(marks, total)
        return Next(
            m, idx, countless = false, allDone = idx == null,
            label = idx?.let { StudyTemplates.unitName(m.template, m.unitLabel, it) }.orEmpty(),
            link = idx?.let { StudyTemplates.linkFor(m.template, m.link, m.issue, it, null) },
        )
    }

    suspend fun bibleNext(app: JwHabitsApp): Next? = next(app, AppDatabase.MATERIAL_BIBLE_READING)

    /** 今使っている資料の「次」。選んでいなければ null。 */
    suspend fun studyNext(app: JwHabitsApp): Next? {
        val id = app.studyPrefs.current.value
        if (id == StudyPrefs.NONE) return null
        return next(app, id)
    }

    data class Timer(val startMs: Long?, val monthMinutes: Long)

    suspend fun timer(app: JwHabitsApp): Timer {
        val ym = YearMonth.from(app.clock.today())
        val now = System.currentTimeMillis()
        val records = app.db.serviceDao().getRange(ym.atDay(1), ym.atEndOfMonth())
        return Timer(app.db.serviceDao().getRunning()?.startEpochMs, ServiceMath.totalMinutes(records, now))
    }
}

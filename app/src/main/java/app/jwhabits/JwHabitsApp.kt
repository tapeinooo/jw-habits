package app.jwhabits

import android.app.Application
import app.jwhabits.data.AppDatabase
import app.jwhabits.data.CalendarRepository
import app.jwhabits.data.backup.BackupRepository
import app.jwhabits.data.ServicePrefs
import app.jwhabits.data.ServiceRepository
import app.jwhabits.data.StudyPrefs
import app.jwhabits.data.StudyRepository
import app.jwhabits.data.UnitRepository
import app.jwhabits.domain.DayClock
import app.jwhabits.service.TimerNotification
import app.jwhabits.widget.DayRollover
import app.jwhabits.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.FlowPreview
import androidx.room.InvalidationTracker
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

class JwHabitsApp : Application() {
    val db: AppDatabase by lazy { AppDatabase.build(this) }
    val clock: DayClock by lazy { DayClock(this) }
    val studyPrefs: StudyPrefs by lazy { StudyPrefs(this) }
    val study: StudyRepository by lazy { StudyRepository(db, clock, studyPrefs) }
    val servicePrefs: ServicePrefs by lazy { ServicePrefs(this) }
    val service: ServiceRepository by lazy { ServiceRepository(db, clock) }
    val backup: BackupRepository by lazy { BackupRepository(this, db, clock, studyPrefs, servicePrefs) }
    val calendar: CalendarRepository by lazy { CalendarRepository(db, clock) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // タイマーが動いている間だけ通知を出す。アプリ起動時にも出し直す。
        observeForWidgets()
        appScope.launch {
            service.running.collect { r ->
                val start = r?.startEpochMs
                if (start != null) TimerNotification.show(this@JwHabitsApp, start)
                else TimerNotification.cancel(this@JwHabitsApp)
            }
        }
    }

    private fun tableChanges(first: String, vararg rest: String): Flow<Unit> = callbackFlow {
        val observer = object : InvalidationTracker.Observer(first, *rest) {
            override fun onInvalidated(tables: Set<String>) { trySend(Unit) }
        }
        db.invalidationTracker.addObserver(observer)
        awaitClose { db.invalidationTracker.removeObserver(observer) }
    }

    /** 記録・資料・今使う資料・日付が変わったら、ウィジェットを描き直す。切り替えの時刻にも描き直す。 */
    @OptIn(FlowPreview::class)
    private fun observeForWidgets() {
        appScope.launch {
            merge(
                tableChanges("unit_record", "day_record", "service_record", "material"),
                studyPrefs.current.map { },
                clock.todayFlow.map { },
            ).debounce(300).collect { WidgetUpdater.updateAll(this@JwHabitsApp) }
        }
        appScope.launch { clock.cutoffFlow.collect { DayRollover.schedule(this@JwHabitsApp) } }
    }

    /** 聖書通読は「今日研究した」に影響しない。個人研究の資料は影響する。 */
    fun unitRepo(materialId: Long): UnitRepository = UnitRepository(
        db, clock, materialId,
        if (materialId == AppDatabase.MATERIAL_BIBLE_READING) null else AppDatabase.HABIT_STUDY,
    )
}

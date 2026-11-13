package app.jwhabits

import android.app.Application
import app.jwhabits.data.AppDatabase
import app.jwhabits.data.ServicePrefs
import app.jwhabits.data.ServiceRepository
import app.jwhabits.data.StudyPrefs
import app.jwhabits.data.StudyRepository
import app.jwhabits.data.UnitRepository
import app.jwhabits.domain.DayClock
import app.jwhabits.service.TimerNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class JwHabitsApp : Application() {
    val db: AppDatabase by lazy { AppDatabase.build(this) }
    val clock: DayClock by lazy { DayClock(this) }
    val studyPrefs: StudyPrefs by lazy { StudyPrefs(this) }
    val study: StudyRepository by lazy { StudyRepository(db, clock, studyPrefs) }
    val servicePrefs: ServicePrefs by lazy { ServicePrefs(this) }
    val service: ServiceRepository by lazy { ServiceRepository(db, clock) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // タイマーが動いている間だけ通知を出す。アプリ起動時にも出し直す。
        appScope.launch {
            service.running.collect { r ->
                val start = r?.startEpochMs
                if (start != null) TimerNotification.show(this@JwHabitsApp, start)
                else TimerNotification.cancel(this@JwHabitsApp)
            }
        }
    }

    /** 聖書通読は「今日研究した」に影響しない。個人研究の資料は影響する。 */
    fun unitRepo(materialId: Long): UnitRepository = UnitRepository(
        db, clock, materialId,
        if (materialId == AppDatabase.MATERIAL_BIBLE_READING) null else AppDatabase.HABIT_STUDY,
    )
}

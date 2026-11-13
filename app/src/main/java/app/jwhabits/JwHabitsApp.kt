package app.jwhabits

import android.app.Application
import app.jwhabits.data.AppDatabase
import app.jwhabits.data.StudyPrefs
import app.jwhabits.data.StudyRepository
import app.jwhabits.data.UnitRepository
import app.jwhabits.domain.DayClock

class JwHabitsApp : Application() {
    val db: AppDatabase by lazy { AppDatabase.build(this) }
    val clock: DayClock by lazy { DayClock(this) }
    val studyPrefs: StudyPrefs by lazy { StudyPrefs(this) }
    val study: StudyRepository by lazy { StudyRepository(db, clock, studyPrefs) }

    /** 聖書通読は「今日研究した」に影響しない。個人研究の資料は影響する。 */
    fun unitRepo(materialId: Long): UnitRepository = UnitRepository(
        db, clock, materialId,
        if (materialId == AppDatabase.MATERIAL_BIBLE_READING) null else AppDatabase.HABIT_STUDY,
    )
}

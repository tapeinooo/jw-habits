package app.jwhabits.data

import app.jwhabits.domain.DayClock
import app.jwhabits.domain.ServiceMath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

/** カレンダーの印に使う、記録のある日の集まり。 */
class CalendarRepository(db: AppDatabase, clock: DayClock) {
    private val units = db.unitRecordDao()
    private val days = db.dayRecordDao()
    private val service = db.serviceDao()

    val todayFlow: Flow<LocalDate> = clock.todayFlow

    /** 聖書通読の記録がある日（どの周でも）。 */
    val bibleDates: Flow<Set<LocalDate>> =
        units.observeDates(AppDatabase.MATERIAL_BIBLE_READING).map { it.toSet() }

    /** 「今日研究した」を付けた日。 */
    val studyDates: Flow<Set<LocalDate>> = days.observeDates(AppDatabase.HABIT_STUDY).map { it.toSet() }

    /** 奉仕の時間がある日（その月）。0分の記録は数えない。 */
    fun serviceDates(month: YearMonth): Flow<Set<LocalDate>> =
        service.observeRange(month.atDay(1), month.atEndOfMonth()).map { list ->
            val now = System.currentTimeMillis()
            list.filter { ServiceMath.durationMinutes(it, now) > 0 }.mapTo(HashSet()) { it.date }
        }
}

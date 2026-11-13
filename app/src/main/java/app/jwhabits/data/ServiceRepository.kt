package app.jwhabits.data

import app.jwhabits.domain.DayClock
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

/** 奉仕の記録（タイマー・分数）と、月ごとの研究件数・補助開拓の印。 */
class ServiceRepository(db: AppDatabase, private val clock: DayClock) {
    private val dao = db.serviceDao()

    fun today(): LocalDate = clock.today()

    val todayFlow: Flow<LocalDate> = clock.todayFlow

    val running: Flow<ServiceRecordEntity?> = dao.observeRunning()

    fun records(from: LocalDate, to: LocalDate): Flow<List<ServiceRecordEntity>> = dao.observeRange(from, to)

    fun month(ym: YearMonth): Flow<ServiceMonthEntity?> = dao.observeMonth(ym.toString())

    /** タイマーを始める。すでに動いていれば何もしない。 */
    suspend fun start(nowMs: Long = System.currentTimeMillis()) {
        if (dao.getRunning() != null) return
        val date = clock.today(java.time.Instant.ofEpochMilli(nowMs).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
        dao.insert(ServiceRecordEntity(date = date, startEpochMs = nowMs))
    }

    /** タイマーを止める。止めた記録を返す（動いていなければ null）。 */
    suspend fun stop(nowMs: Long = System.currentTimeMillis()): ServiceRecordEntity? {
        val r = dao.getRunning() ?: return null
        val stopped = r.copy(endEpochMs = maxOf(nowMs, r.startEpochMs ?: nowMs))
        dao.update(stopped)
        return stopped
    }

    suspend fun addMinutes(date: LocalDate, minutes: Int): Long =
        dao.insert(ServiceRecordEntity(date = date, minutes = minutes))

    suspend fun update(r: ServiceRecordEntity) = dao.update(r)

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun restore(r: ServiceRecordEntity) = dao.insert(r)

    suspend fun setBibleStudies(ym: YearMonth, count: Int) {
        val cur = dao.getMonth(ym.toString()) ?: ServiceMonthEntity(ym.toString())
        dao.upsertMonth(cur.copy(bibleStudies = count.coerceAtLeast(0)))
    }

    suspend fun setAuxiliary(ym: YearMonth, on: Boolean) {
        val cur = dao.getMonth(ym.toString()) ?: ServiceMonthEntity(ym.toString())
        dao.upsertMonth(cur.copy(role = if (on) ServiceRole.AUXILIARY_PIONEER else ServiceRole.REGULAR_PIONEER))
    }
}

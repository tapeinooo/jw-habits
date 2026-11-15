package app.jwhabits.data

import app.jwhabits.domain.DayClock
import app.jwhabits.domain.ServiceMath
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

    /**
     * その日の「手で入れた」記録（分数だけの記録。複数なら一番新しいもの）に deltaMinutes を足す。
     * 0になれば記録を消す。記録が無くて減らす場合は何もしない。
     */
    suspend fun adjustDay(date: LocalDate, deltaMinutes: Int) {
        val manual = dao.getRange(date, date)
            .filter { it.minutes != null && it.startEpochMs == null && it.endEpochMs == null }
            .maxByOrNull { it.id }
        if (manual == null && deltaMinutes <= 0) return
        val next = ServiceMath.applyDelta(manual?.minutes, deltaMinutes)
        when {
            manual == null -> dao.insert(ServiceRecordEntity(date = date, minutes = next))
            next == 0 -> dao.delete(manual.id)
            else -> dao.update(manual.copy(minutes = next))
        }
    }

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

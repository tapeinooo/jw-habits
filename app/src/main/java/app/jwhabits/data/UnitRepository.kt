package app.jwhabits.data

import app.jwhabits.domain.DayClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class ReadingSnapshot(val material: MaterialEntity, val records: List<UnitRecordEntity>)

/**
 * 1つの資料の単位（章・研究・記事）の記録。聖書通読も個人研究の資料も同じ仕組み。
 * studyHabitId があれば、単位を記録したとき「今日研究した」も入れる（聖書通読は null）。
 */
class UnitRepository(
    db: AppDatabase,
    private val clock: DayClock,
    private val materialId: Long,
    private val studyHabitId: Long?,
) {
    private val materials = db.materialDao()
    private val units = db.unitRecordDao()
    private val days = db.dayRecordDao()

    fun today(): LocalDate = clock.today()

    val todayFlow: Flow<LocalDate> = clock.todayFlow

    @OptIn(ExperimentalCoroutinesApi::class)
    val snapshot: Flow<ReadingSnapshot> =
        materials.observe(materialId).filterNotNull().flatMapLatest { m ->
            units.observeRound(materialId, m.round).map { ReadingSnapshot(m, it) }
        }

    private suspend fun material(): MaterialEntity = materials.get(materialId)!!

    /** 今日の「研究した」を足す。今回足したときだけ、取り消し用の処理を返す。 */
    private suspend fun addStudyDay(): (suspend () -> Unit)? {
        val habit = studyHabitId ?: return null
        val date = today()
        return if (days.insert(DayRecordEntity(habit, date)) != -1L) ({ days.delete(habit, date) }) else null
    }

    private fun Undo.alsoUndo(extra: (suspend () -> Unit)?): Undo =
        if (extra == null) this else Undo(marked) { run(); extra() }

    /** 未記録なら今日の日付で記録、記録済みなら取り消す。元に戻す処理を返す。 */
    suspend fun toggle(index: Int): Undo {
        val r = material().round
        val existing = units.get(materialId, r, index)
        return if (existing == null) {
            val ids = units.insertAll(listOf(UnitRecordEntity(materialId = materialId, round = r, unitIndex = index, date = today())))
            Undo(marked = 1) { units.deleteByIds(ids.filter { it > 0 }) }.alsoUndo(addStudyDay())
        } else {
            units.deleteByIds(listOf(existing.id))
            Undo(marked = -1) { units.insertAll(listOf(existing)) }
        }
    }

    /** 範囲をまとめて記録（記録済みはそのまま）。小さい番号から入れるので「最後に記録した単位」は範囲の末尾になる。 */
    suspend fun markAll(indices: Collection<Int>, date: LocalDate = today()): Undo {
        val r = material().round
        val ids = units.insertAll(
            indices.sorted().map { UnitRecordEntity(materialId = materialId, round = r, unitIndex = it, date = date) }
        ).filter { it > 0 }
        val undo = Undo(marked = ids.size) { units.deleteByIds(ids) }
        return if (ids.isEmpty()) undo else undo.alsoUndo(addStudyDay())
    }

    /** 記録した日を直す。未記録の単位なら、その日付で記録する。 */
    suspend fun setDate(index: Int, date: LocalDate) {
        val r = material().round
        val existing = units.get(materialId, r, index)
        if (existing == null) {
            units.insertAll(listOf(UnitRecordEntity(materialId = materialId, round = r, unitIndex = index, date = date)))
        } else {
            units.update(existing.copy(date = date))
        }
    }

    /** 全部終えたら次の周へ。前の周の記録は残る。 */
    suspend fun startNextRound() {
        val m = material()
        val total = checkNotNull(m.unitCount) { "no unit count" }
        val done = units.getRound(materialId, m.round).count { it.unitIndex < total }
        check(done >= total) { "round not finished" }
        materials.update(m.copy(round = m.round + 1))
    }

    /** 数を決めない資料に、今日の記録を1件足す。番号は（今の周の最大＋1）。 */
    suspend fun addLog(link: String? = null): Undo {
        val r = material().round
        val next = (units.maxIndex(materialId, r) ?: -1) + 1
        val ids = units.insertAll(
            listOf(UnitRecordEntity(materialId = materialId, round = r, unitIndex = next, date = today(), link = link))
        ).filter { it > 0 }
        return Undo(marked = ids.size) { units.deleteByIds(ids) }.alsoUndo(addStudyDay())
    }

    suspend fun removeLog(record: UnitRecordEntity): Undo {
        units.deleteByIds(listOf(record.id))
        return Undo(marked = -1) { units.insertAll(listOf(record)) }
    }

    /**
     * 共有で受け取ったリンクを記録する。
     * 数を決めない資料なら記録を1件足す。単位を選んでいればその単位を記録済みにしてリンクを付ける
     * （すでに記録済みならリンクだけ付ける）。どちらでもなければ資料のリンクにする。
     * いずれも「今日研究した」を入れる。
     */
    suspend fun recordShare(unitIndex: Int?, link: String?): Undo {
        val m = material()
        val base: Undo = when {
            m.unitCount == null -> return addLog(link)
            unitIndex != null -> {
                val existing = units.get(materialId, m.round, unitIndex)
                if (existing == null) {
                    val ids = units.insertAll(
                        listOf(UnitRecordEntity(materialId = materialId, round = m.round, unitIndex = unitIndex, date = today(), link = link))
                    ).filter { it > 0 }
                    Undo(marked = ids.size) { units.deleteByIds(ids) }
                } else {
                    units.update(existing.copy(link = link))
                    Undo(marked = 0) { units.update(existing) }
                }
            }
            else -> {
                materials.update(m.copy(link = link))
                Undo(marked = 0) { materials.update(m) }
            }
        }
        return base.alsoUndo(addStudyDay())
    }

    class Undo(val marked: Int, val run: suspend () -> Unit)
}

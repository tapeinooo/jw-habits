package app.jwhabits.data

import app.jwhabits.domain.BibleBooks
import app.jwhabits.domain.DayClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class ReadingSnapshot(val material: MaterialEntity, val records: List<UnitRecordEntity>)

/** 聖書通読の記録。聖書通読は「1189章の資料」として material/unit_record に入る。 */
class BibleRepository(db: AppDatabase, private val clock: DayClock) {
    private val materials = db.materialDao()
    private val units = db.unitRecordDao()
    private val materialId = AppDatabase.MATERIAL_BIBLE_READING

    fun today(): LocalDate = clock.today()

    @OptIn(ExperimentalCoroutinesApi::class)
    val snapshot: Flow<ReadingSnapshot> =
        materials.observe(materialId).filterNotNull().flatMapLatest { m ->
            units.observeRound(materialId, m.round).map { ReadingSnapshot(m, it) }
        }

    private suspend fun round(): Int = materials.get(materialId)!!.round

    /** 未読なら今日の日付で記録、既読なら取り消す。元に戻す処理を返す。 */
    suspend fun toggle(index: Int): Undo {
        val r = round()
        val existing = units.get(materialId, r, index)
        return if (existing == null) {
            val ids = units.insertAll(listOf(UnitRecordEntity(materialId = materialId, round = r, unitIndex = index, date = today())))
            Undo(marked = 1) { units.deleteByIds(ids.filter { it > 0 }) }
        } else {
            units.deleteByIds(listOf(existing.id))
            Undo(marked = -1) { units.insertAll(listOf(existing)) }
        }
    }

    /** 範囲をまとめて記録（既読の章はそのまま）。小さい番号から入れるので「最後に記録した章」は範囲の末尾になる。 */
    suspend fun markAll(indices: Collection<Int>, date: LocalDate = today()): Undo {
        val r = round()
        val ids = units.insertAll(
            indices.sorted().map { UnitRecordEntity(materialId = materialId, round = r, unitIndex = it, date = date) }
        ).filter { it > 0 }
        return Undo(marked = ids.size) { units.deleteByIds(ids) }
    }

    /** 読んだ日を直す。未読の章なら、その日付で記録する。 */
    suspend fun setDate(index: Int, date: LocalDate) {
        val r = round()
        val existing = units.get(materialId, r, index)
        if (existing == null) {
            units.insertAll(listOf(UnitRecordEntity(materialId = materialId, round = r, unitIndex = index, date = date)))
        } else {
            units.update(existing.copy(date = date))
        }
    }

    /** 全章を読み終えたら次の周へ。前の周の記録は残る。 */
    suspend fun startNextRound() {
        val m = materials.get(materialId)!!
        val done = units.getRound(materialId, m.round).size
        check(done >= BibleBooks.totalChapters) { "round not finished" }
        materials.update(m.copy(round = m.round + 1))
    }

    class Undo(val marked: Int, val run: suspend () -> Unit)
}

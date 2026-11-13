package app.jwhabits.ui.units

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.jwhabits.JwHabitsApp
import app.jwhabits.data.MaterialEntity
import app.jwhabits.data.UnitRecordEntity
import app.jwhabits.data.UnitRepository
import app.jwhabits.domain.BibleBook
import app.jwhabits.domain.ReadMark
import app.jwhabits.domain.ReadingProgress
import app.jwhabits.domain.StudyTemplates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class UnitUiState(
    val loaded: Boolean = false,
    val material: MaterialEntity? = null,
    val round: Int = 1,
    /** 単位の数。数を決めない資料は 0 */
    val total: Int = 0,
    /** 単位の番号（0始まり） → 記録 */
    val read: Map<Int, UnitRecordEntity> = emptyMap(),
    /** 数を決めない資料の記録（新しい順） */
    val log: List<UnitRecordEntity> = emptyList(),
    val nextIndex: Int? = null,
    val todayCount: Int = 0,
    val today: LocalDate = LocalDate.now(),
) {
    val countless: Boolean get() = material != null && material.unitCount == null
    val readCount: Int get() = read.keys.count { it < total }
    val allDone: Boolean get() = loaded && !countless && total > 0 && readCount >= total
    fun readIn(book: BibleBook): Int = (book.firstIndex until book.firstIndex + book.chapters).count { it in read }

    fun nameOf(index: Int): String =
        StudyTemplates.unitName(material?.template, material?.unitLabel.orEmpty(), index)

    fun linkFor(index: Int?): String? = material?.let {
        StudyTemplates.linkFor(it.template, it.link, it.issue, index, index?.let(read::get)?.link)
    }
}

typealias OfferUndo = (String, suspend () -> Unit) -> Unit

class UnitViewModel(private val repo: UnitRepository) : ViewModel() {

    val state: StateFlow<UnitUiState> = combine(repo.snapshot, repo.todayFlow) { s, today ->
        val total = s.material.unitCount ?: 0
        UnitUiState(
            loaded = true,
            material = s.material,
            round = s.material.round,
            total = total,
            read = s.records.associateBy { it.unitIndex },
            log = if (s.material.unitCount == null) s.records.sortedWith(compareByDescending<UnitRecordEntity> { it.date }.thenByDescending { it.unitIndex }) else emptyList(),
            nextIndex = ReadingProgress.nextUnread(s.records.map { ReadMark(it.id, it.unitIndex) }, total),
            todayCount = s.records.count { it.date == today },
            today = today,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnitUiState())

    /** 記録して、元に戻す処理を onUndo に渡す。 */
    fun toggle(index: Int, onUndo: OfferUndo) = viewModelScope.launch {
        val label = state.value.nameOf(index)
        val undo = repo.toggle(index)
        onUndo(if (undo.marked > 0) "$label を記録しました" else "$label の記録を取り消しました", undo.run)
    }

    fun markRange(indices: Collection<Int>, onUndo: OfferUndo) = viewModelScope.launch {
        val unit = state.value.material?.unitLabel.orEmpty()
        val undo = repo.markAll(indices)
        if (undo.marked > 0) onUndo("${undo.marked}${unit}をまとめて記録しました", undo.run)
    }

    fun addLog(onUndo: OfferUndo) = viewModelScope.launch {
        val undo = repo.addLog()
        if (undo.marked > 0) onUndo("今日の記録を追加しました", undo.run)
    }

    fun removeLog(record: UnitRecordEntity, onUndo: OfferUndo) = viewModelScope.launch {
        val undo = repo.removeLog(record)
        onUndo("記録を削除しました", undo.run)
    }

    fun setDate(index: Int, date: LocalDate) = viewModelScope.launch { repo.setDate(index, date) }

    fun startNextRound() = viewModelScope.launch { repo.startNextRound() }

    fun today(): LocalDate = repo.today()

    companion object {
        fun factory(materialId: Long) = viewModelFactory {
            initializer { UnitViewModel((this[APPLICATION_KEY] as JwHabitsApp).unitRepo(materialId)) }
        }

        fun key(materialId: Long) = "unit-$materialId"
    }
}

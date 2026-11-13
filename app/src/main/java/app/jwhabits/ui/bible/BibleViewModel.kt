package app.jwhabits.ui.bible

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.jwhabits.JwHabitsApp
import app.jwhabits.data.BibleRepository
import app.jwhabits.data.UnitRecordEntity
import app.jwhabits.domain.BibleBook
import app.jwhabits.domain.BibleBooks
import app.jwhabits.domain.ChapterRef
import app.jwhabits.domain.ReadMark
import app.jwhabits.domain.ReadingProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BibleUiState(
    val loaded: Boolean = false,
    val round: Int = 1,
    /** 章の通し番号 → 記録 */
    val read: Map<Int, UnitRecordEntity> = emptyMap(),
    val next: ChapterRef? = null,
    val todayCount: Int = 0,
) {
    val readCount: Int get() = read.size
    val allDone: Boolean get() = loaded && read.size >= BibleBooks.totalChapters
    fun readIn(book: BibleBook): Int = (book.firstIndex until book.firstIndex + book.chapters).count { it in read }
}

class BibleViewModel(private val repo: BibleRepository) : ViewModel() {

    val state: StateFlow<BibleUiState> = repo.snapshot.map { s ->
        val today = repo.today()
        val read = s.records.associateBy { it.unitIndex }
        val next = ReadingProgress.nextUnread(s.records.map { ReadMark(it.id, it.unitIndex) }, BibleBooks.totalChapters)
        BibleUiState(
            loaded = true,
            round = s.material.round,
            read = read,
            next = next?.let(BibleBooks::chapterAt),
            todayCount = s.records.count { it.date == today },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BibleUiState())

    /** 記録して、元に戻す処理を onUndo に渡す。 */
    fun toggle(index: Int, onUndo: (String, suspend () -> Unit) -> Unit) = viewModelScope.launch {
        val undo = repo.toggle(index)
        val label = BibleBooks.chapterAt(index).label
        onUndo(if (undo.marked > 0) "$label を記録しました" else "$label の記録を取り消しました", undo.run)
    }

    fun markRange(indices: Collection<Int>, onUndo: (String, suspend () -> Unit) -> Unit) = viewModelScope.launch {
        val undo = repo.markAll(indices)
        if (undo.marked > 0) onUndo("${undo.marked}章をまとめて記録しました", undo.run)
    }

    fun setDate(index: Int, date: LocalDate) = viewModelScope.launch { repo.setDate(index, date) }

    fun startNextRound() = viewModelScope.launch { repo.startNextRound() }

    fun today(): LocalDate = repo.today()

    companion object {
        val Factory = viewModelFactory {
            initializer { BibleViewModel((this[APPLICATION_KEY] as JwHabitsApp).bible) }
        }
    }
}

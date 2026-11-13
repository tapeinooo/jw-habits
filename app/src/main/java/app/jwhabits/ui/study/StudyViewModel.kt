package app.jwhabits.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.jwhabits.JwHabitsApp
import app.jwhabits.data.MaterialEntity
import app.jwhabits.data.StudyPrefs
import app.jwhabits.data.StudyRepository
import app.jwhabits.domain.StudyTemplates
import app.jwhabits.ui.units.OfferUndo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 資料の一覧の1行。done は今の周で記録した数。 */
data class MaterialItem(val material: MaterialEntity, val done: Int)

class StudyViewModel(
    private val repo: StudyRepository,
    private val app: JwHabitsApp,
    private val prefs: StudyPrefs,
) : ViewModel() {

    val currentId: StateFlow<Long> = prefs.current

    val studiedToday: StateFlow<Boolean> =
        repo.studiedToday().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val materials: StateFlow<List<MaterialItem>> =
        combine(repo.activeMaterials, repo.counts) { list, counts ->
            val byId = counts.associate { it.materialId to it.count }
            list.map { MaterialItem(it, byId[it.id] ?: 0) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun today(): LocalDate = repo.today()

    fun toggleToday(onUndo: OfferUndo) = viewModelScope.launch {
        val undo = repo.toggleToday()
        onUndo(if (undo.marked > 0) "今日研究したを記録しました" else "今日研究したを取り消しました", undo.run)
    }

    fun create(spec: StudyTemplates.Spec, onCreated: (Long) -> Unit = {}) = viewModelScope.launch {
        onCreated(repo.create(spec))
    }

    fun update(material: MaterialEntity) = viewModelScope.launch { repo.update(material) }

    fun setCurrent(id: Long) = prefs.set(id)

    fun clearCurrent() = prefs.clear()

    fun archive(id: Long) = viewModelScope.launch { repo.archive(id) }

    /** 次の号を足す。足したら、その資料を返す（すでにあれば null）。 */
    fun addNextIssue(from: MaterialEntity, onResult: (MaterialEntity?) -> Unit) = viewModelScope.launch {
        onResult(repo.addNextIssue(from))
    }

    /** 共有で受け取ったリンクを記録する。 */
    fun saveShare(materialId: Long, unitIndex: Int?, link: String?, onUndo: OfferUndo) = viewModelScope.launch {
        val undo = app.unitRepo(materialId).recordShare(unitIndex, link)
        onUndo("記録しました", undo.run)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as JwHabitsApp
                StudyViewModel(app.study, app, app.studyPrefs)
            }
        }
    }
}

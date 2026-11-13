package app.jwhabits.data

import app.jwhabits.domain.DayClock
import app.jwhabits.domain.StudyTemplates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate

/** 個人研究の資料の一覧・追加・変更と、「今日研究した」。単位の記録は UnitRepository。 */
class StudyRepository(db: AppDatabase, private val clock: DayClock, private val prefs: StudyPrefs) {
    private val materials = db.materialDao()
    private val units = db.unitRecordDao()
    private val days = db.dayRecordDao()
    private val habit = AppDatabase.HABIT_STUDY

    fun today(): LocalDate = clock.today()

    val activeMaterials: Flow<List<MaterialEntity>> = materials.observeByHabit(habit)
    val counts: Flow<List<MaterialCount>> = units.observeCurrentCounts(habit)
    @OptIn(ExperimentalCoroutinesApi::class)
    fun studiedToday(): Flow<Boolean> = clock.todayFlow.flatMapLatest { days.observeExists(habit, it) }

    /** 「今日研究した」を付ける／外す。 */
    suspend fun toggleToday(): UnitRepository.Undo {
        val date = today()
        val record = DayRecordEntity(habit, date)
        return if (days.insert(record) != -1L) {
            UnitRepository.Undo(marked = 1) { days.delete(habit, date) }
        } else {
            days.delete(habit, date)
            UnitRepository.Undo(marked = -1) { days.insert(record) }
        }
    }

    suspend fun create(spec: StudyTemplates.Spec): Long = materials.insert(
        MaterialEntity(
            habitId = habit, title = spec.title, unitLabel = spec.unitLabel, unitCount = spec.unitCount,
            template = spec.template, issue = spec.issue, sortOrder = materials.maxSortOrder(habit) + 1,
        )
    )

    suspend fun update(material: MaterialEntity) = materials.update(material)

    /** 資料を外す（記録は残す）。今使っている資料だったら選択も外す。 */
    suspend fun archive(id: Long) {
        val m = materials.get(id) ?: return
        materials.update(m.copy(archived = true))
        if (prefs.current.value == id) prefs.clear()
    }

    /** ものみの塔の次の号を、同じ数の記事で作る。すでにあれば null。 */
    suspend fun addNextIssue(from: MaterialEntity): MaterialEntity? {
        val (y, m) = StudyTemplates.parseIssue(from.issue) ?: return null
        val (ny, nm) = StudyTemplates.nextIssue(y, m)
        val key = StudyTemplates.issueKey(ny, nm)
        if (materials.getByHabit(habit).any { it.issue == key }) return null
        val spec = StudyTemplates.watchtower(ny, nm, from.unitCount ?: StudyTemplates.WT_DEFAULT_COUNT)
        val id = create(spec)
        return materials.get(id)
    }
}

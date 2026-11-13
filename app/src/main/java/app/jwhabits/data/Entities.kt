package app.jwhabits.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** 記録の種類。習慣はあとから足せる（日々の聖句・集会の予習など）。 */
enum class HabitKind { CHAPTER_GRID, MATERIAL, TIME }

@Entity(tableName = "habit")
data class HabitEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val kind: HabitKind,
    val sortOrder: Int,
)

/** ひな形の種類。null は手で作った資料。 */
enum class MaterialTemplate { BIBLE_READING, BIBLE_STUDY, BE, WATCHTOWER }

/** 資料。聖書通読も「1189単位の資料」として持つ。 */
@Entity(
    tableName = "material",
    foreignKeys = [ForeignKey(HabitEntity::class, ["id"], ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("habitId")],
)
data class MaterialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val title: String,
    /** 単位の呼び名（章・課・研究・記事） */
    val unitLabel: String,
    /** 単位の数。null は数を決めない「記録だけの資料」 */
    val unitCount: Int?,
    val link: String? = null,
    val template: MaterialTemplate? = null,
    /** 今の周（1始まり） */
    val round: Int = 1,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
)

/** 単位の記録（章・研究・記事を読んだ日）。前の周の記録も残す。 */
@Entity(
    tableName = "unit_record",
    foreignKeys = [ForeignKey(MaterialEntity::class, ["id"], ["materialId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["materialId", "round", "unitIndex"], unique = true), Index("date")],
)
data class UnitRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val materialId: Long,
    val round: Int,
    /** 0始まり。聖書通読は全1189章を通した番号 */
    val unitIndex: Int,
    val date: LocalDate,
    val link: String? = null,
)

/** 日の記録（「今日研究した」など）。 */
@Entity(
    tableName = "day_record",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [ForeignKey(HabitEntity::class, ["id"], ["habitId"], onDelete = ForeignKey.CASCADE)],
)
data class DayRecordEntity(
    val habitId: Long,
    val date: LocalDate,
)

/** 奉仕の記録。開始と終了、または分数。 */
@Entity(tableName = "service_record", indices = [Index("date")])
data class ServiceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val startEpochMs: Long? = null,
    val endEpochMs: Long? = null,
    val minutes: Int? = null,
)

enum class ServiceRole { REGULAR_PIONEER, AUXILIARY_PIONEER, PUBLISHER }

/** 奉仕の月。yearMonth は "2026-11" の形。 */
@Entity(tableName = "service_month")
data class ServiceMonthEntity(
    @PrimaryKey val yearMonth: String,
    val bibleStudies: Int = 0,
    val role: ServiceRole = ServiceRole.REGULAR_PIONEER,
)

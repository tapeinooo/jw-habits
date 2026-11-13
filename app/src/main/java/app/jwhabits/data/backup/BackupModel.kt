package app.jwhabits.data.backup

import app.jwhabits.data.DayRecordEntity
import app.jwhabits.data.HabitEntity
import app.jwhabits.data.HabitKind
import app.jwhabits.data.MaterialEntity
import app.jwhabits.data.MaterialTemplate
import app.jwhabits.data.ServiceMonthEntity
import app.jwhabits.data.ServiceRecordEntity
import app.jwhabits.data.ServiceRole
import app.jwhabits.data.UnitRecordEntity
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate

/** 読み込めないファイル。message はそのまま画面に出す（日本語）。 */
class BackupException(message: String) : Exception(message)

@Serializable
data class BackupSettings(
    val dayCutoffHour: Int = 3,
    /** -1 は未選択 */
    val currentStudyMaterialId: Long = -1L,
    val serviceRole: ServiceRole = ServiceRole.REGULAR_PIONEER,
    val annualGoal: Int = 600,
    val auxiliaryMonthlyGoal: Int = 30,
)

@Serializable
data class HabitDto(val id: Long, val name: String, val kind: HabitKind, val sortOrder: Int)

@Serializable
data class MaterialDto(
    val id: Long,
    val habitId: Long,
    val title: String,
    val unitLabel: String,
    val unitCount: Int? = null,
    val link: String? = null,
    val template: MaterialTemplate? = null,
    val round: Int = 1,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
    val issue: String? = null,
)

@Serializable
data class UnitRecordDto(
    val id: Long,
    val materialId: Long,
    val round: Int,
    val unitIndex: Int,
    val date: String,
    val link: String? = null,
)

@Serializable
data class DayRecordDto(val habitId: Long, val date: String)

@Serializable
data class ServiceRecordDto(
    val id: Long,
    val date: String,
    val startEpochMs: Long? = null,
    val endEpochMs: Long? = null,
    val minutes: Int? = null,
)

@Serializable
data class ServiceMonthDto(
    val yearMonth: String,
    val bibleStudies: Int = 0,
    val role: ServiceRole = ServiceRole.REGULAR_PIONEER,
)

@Serializable
data class BackupFile(
    val format: String = BackupCodec.FORMAT,
    val schemaVersion: Int = BackupCodec.SCHEMA_VERSION,
    val dbVersion: Int = 0,
    val appVersion: String = "",
    val exportedAt: String = "",
    val settings: BackupSettings = BackupSettings(),
    val habits: List<HabitDto>,
    val materials: List<MaterialDto>,
    val unitRecords: List<UnitRecordDto>,
    val dayRecords: List<DayRecordDto>,
    val serviceRecords: List<ServiceRecordDto>,
    val serviceMonths: List<ServiceMonthDto>,
)

// --- entity <-> DTO ---

fun HabitEntity.toDto() = HabitDto(id, name, kind, sortOrder)
fun HabitDto.toEntity() = HabitEntity(id, name, kind, sortOrder)

fun MaterialEntity.toDto() = MaterialDto(id, habitId, title, unitLabel, unitCount, link, template, round, sortOrder, archived, issue)
fun MaterialDto.toEntity() = MaterialEntity(id, habitId, title, unitLabel, unitCount, link, template, round, sortOrder, archived, issue)

fun UnitRecordEntity.toDto() = UnitRecordDto(id, materialId, round, unitIndex, date.toString(), link)
fun UnitRecordDto.toEntity() = UnitRecordEntity(id, materialId, round, unitIndex, parseDate(date), link)

fun DayRecordEntity.toDto() = DayRecordDto(habitId, date.toString())
fun DayRecordDto.toEntity() = DayRecordEntity(habitId, parseDate(date))

fun ServiceRecordEntity.toDto() = ServiceRecordDto(id, date.toString(), startEpochMs, endEpochMs, minutes)
fun ServiceRecordDto.toEntity() = ServiceRecordEntity(id, parseDate(date), startEpochMs, endEpochMs, minutes)

fun ServiceMonthEntity.toDto() = ServiceMonthDto(yearMonth, bibleStudies, role)
fun ServiceMonthDto.toEntity() = ServiceMonthEntity(yearMonth, bibleStudies, role)

private fun parseDate(s: String): LocalDate =
    try { LocalDate.parse(s) } catch (e: Exception) { throw BackupException("日付が正しくありません（$s）") }

/** JSON の読み書きと検証。Android に依存しない。 */
object BackupCodec {
    const val FORMAT = "jw-habits-backup"
    const val SCHEMA_VERSION = 1
    private val REQUIRED_ARRAYS = listOf("habits", "materials", "unitRecords", "dayRecords", "serviceRecords", "serviceMonths")

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    /** 形式・版・必須の配列を調べてから読む。だめなら BackupException（理由つき）。 */
    fun decode(text: String): BackupFile {
        val root = try {
            json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            throw BackupException("JSON として読めません")
        }
        val format = (root["format"] as? JsonPrimitive)?.contentOrNull
        if (format != FORMAT) throw BackupException("JW習慣のバックアップではありません")
        val version = (root["schemaVersion"] as? JsonPrimitive)?.intOrNull
            ?: throw BackupException("ファイルの版が分かりません")
        if (version > SCHEMA_VERSION) throw BackupException("新しい版のアプリで書き出したファイルです（版 $version）")
        if (version < 1) throw BackupException("ファイルの版が正しくありません（版 $version）")
        for (name in REQUIRED_ARRAYS) {
            val ok = try { root[name]?.jsonArray != null } catch (e: Exception) { false }
            if (!ok) throw BackupException("「$name」がありません")
        }
        val file = try {
            json.decodeFromJsonElement(BackupFile.serializer(), JsonObject(root))
        } catch (e: SerializationException) {
            throw BackupException("中身の形が正しくありません")
        } catch (e: IllegalArgumentException) {
            throw BackupException("中身の形が正しくありません")
        }
        validate(file)
        return file
    }

    /** 日付などを実際に変換してみて、読み込み途中で失敗しないようにする。 */
    fun validate(file: BackupFile) {
        file.unitRecords.forEach { it.toEntity() }
        file.dayRecords.forEach { it.toEntity() }
        file.serviceRecords.forEach { it.toEntity() }
    }
}

package app.jwhabits.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import app.jwhabits.data.AppDatabase
import app.jwhabits.data.ServicePrefs
import app.jwhabits.data.StudyPrefs
import app.jwhabits.domain.DayClock
import app.jwhabits.service.TimerNotification
import app.jwhabits.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.OffsetDateTime

/** 書き出し・読み込み。端末の外には出さず、ユーザーが選んだファイルにだけ書く。 */
class BackupRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val clock: DayClock,
    private val studyPrefs: StudyPrefs,
    private val servicePrefs: ServicePrefs,
) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _lastBackup = MutableStateFlow(prefs.getString(KEY_LAST_BACKUP, null))

    /** 最後に書き出した日（YYYY-MM-DD）。まだなら null。 */
    val lastBackupDate: StateFlow<String?> = _lastBackup

    fun defaultFileName(today: LocalDate = LocalDate.now()) = "jw-habits-backup-$today.json"

    suspend fun export(): String {
        val file = BackupFile(
            dbVersion = AppDatabase.VERSION,
            appVersion = appVersion(),
            exportedAt = OffsetDateTime.now().withNano(0).toString(),
            settings = BackupSettings(
                dayCutoffHour = clock.cutoffHour,
                currentStudyMaterialId = studyPrefs.current.value,
                serviceRole = servicePrefs.role.value,
                annualGoal = servicePrefs.annualGoal.value,
                auxiliaryMonthlyGoal = servicePrefs.monthlyGoal.value,
            ),
            habits = db.habitDao().getAll().map { it.toDto() },
            materials = db.materialDao().getAll().map { it.toDto() },
            unitRecords = db.unitRecordDao().getAll().map { it.toDto() },
            dayRecords = db.dayRecordDao().getAll().map { it.toDto() },
            serviceRecords = db.serviceDao().getAllRecords().map { it.toDto() },
            serviceMonths = db.serviceDao().getAllMonths().map { it.toDto() },
        )
        return BackupCodec.encode(file)
    }

    /** export() の結果を uri に書き、書き出した日を覚える。 */
    suspend fun exportTo(uri: Uri) {
        val text = export()
        withContext(Dispatchers.IO) {
            val out = context.contentResolver.openOutputStream(uri, "wt")
                ?: throw BackupException("書き込み先を開けませんでした")
            out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }
        val today = LocalDate.now().toString()
        prefs.edit().putString(KEY_LAST_BACKUP, today).apply()
        _lastBackup.value = today
    }

    /** uri のファイルを読んで検証する。データはまだ変えない。 */
    suspend fun read(uri: Uri): BackupFile {
        val text = withContext(Dispatchers.IO) {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw BackupException("ファイルを開けませんでした")
            input.use { String(it.readBytes(), Charsets.UTF_8) }
        }
        return BackupCodec.decode(text)
    }

    /** 1つのトランザクションで全部入れ替える。失敗したら何も変わらない。そのあと設定とウィジェット・通知を直す。 */
    suspend fun replaceAll(file: BackupFile) {
        val habits = file.habits.map { it.toEntity() }
        val materials = file.materials.map { it.toEntity() }
        val units = file.unitRecords.map { it.toEntity() }
        val days = file.dayRecords.map { it.toEntity() }
        val records = file.serviceRecords.map { it.toEntity() }
        val months = file.serviceMonths.map { it.toEntity() }
        db.withTransaction {
            db.unitRecordDao().deleteAll()
            db.dayRecordDao().deleteAll()
            db.materialDao().deleteAll()
            db.habitDao().deleteAll()
            db.serviceDao().deleteAllRecords()
            db.serviceDao().deleteAllMonths()
            db.habitDao().restoreAll(habits)
            db.materialDao().restoreAll(materials)
            db.unitRecordDao().restoreAll(units)
            db.dayRecordDao().restoreAll(days)
            db.serviceDao().restoreRecords(records)
            db.serviceDao().restoreMonths(months)
        }
        val s = file.settings
        clock.cutoffHour = s.dayCutoffHour
        servicePrefs.setRole(s.serviceRole)
        servicePrefs.setAnnualGoal(s.annualGoal)
        servicePrefs.setMonthlyGoal(s.auxiliaryMonthlyGoal)
        if (s.currentStudyMaterialId != StudyPrefs.NONE && db.materialDao().get(s.currentStudyMaterialId) != null) {
            studyPrefs.set(s.currentStudyMaterialId)
        } else {
            studyPrefs.clear()
        }
        WidgetUpdater.updateAll(context)
        val start = db.serviceDao().getRunning()?.startEpochMs
        if (start != null) TimerNotification.show(context, start) else TimerNotification.cancel(context)
    }

    private fun appVersion(): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""

    companion object {
        private const val KEY_LAST_BACKUP = "last_backup_date"
    }
}

package app.jwhabits.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.jwhabits.JwHabitsApp
import app.jwhabits.data.backup.BackupException
import app.jwhabits.data.backup.BackupFile
import app.jwhabits.data.backup.BackupRepository
import app.jwhabits.domain.DayClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 読み込み確認の表示用。 */
data class PendingImport(
    val file: BackupFile,
    val exportedAt: String,
    val unitRecords: Int,
    val materials: Int,
    val serviceRecords: Int,
)

class SettingsViewModel(private val backup: BackupRepository, private val clock: DayClock) : ViewModel() {
    val cutoffHour: StateFlow<Int> = clock.cutoffFlow
    val lastBackupDate: StateFlow<String?> = backup.lastBackupDate

    private val _pending = MutableStateFlow<PendingImport?>(null)
    val pending: StateFlow<PendingImport?> = _pending

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun setCutoff(hour: Int) { clock.cutoffHour = hour }

    fun defaultFileName(today: LocalDate) = backup.defaultFileName(today)

    fun versionName(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            try {
                backup.exportTo(uri)
                _messages.trySend("書き出しました")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.trySend("書き出せませんでした：${e.message ?: "原因不明"}")
            } finally {
                _busy.value = false
            }
        }
    }

    fun readForImport(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            try {
                val f = backup.read(uri)
                _pending.value = PendingImport(f, formatExportedAt(f.exportedAt), f.unitRecords.size, f.materials.size, f.serviceRecords.size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.trySend("読み込めませんでした：${reason(e)}")
            } finally {
                _busy.value = false
            }
        }
    }

    fun cancelImport() { _pending.value = null }

    fun confirmImport() {
        val p = _pending.value ?: return
        _pending.value = null
        viewModelScope.launch {
            _busy.value = true
            try {
                backup.replaceAll(p.file)
                _messages.trySend("読み込みました")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.trySend("読み込めませんでした：${reason(e)}")
            } finally {
                _busy.value = false
            }
        }
    }

    private fun reason(e: Exception) = if (e is BackupException) e.message ?: "原因不明" else e.message ?: "原因不明"

    private fun formatExportedAt(s: String): String = runCatching {
        OffsetDateTime.parse(s).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }.getOrDefault(s.ifEmpty { "不明" })

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as JwHabitsApp
                SettingsViewModel(app.backup, app.clock)
            }
        }
    }
}

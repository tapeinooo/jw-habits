package app.jwhabits.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import app.jwhabits.domain.SharedPayload
import app.jwhabits.ui.bible.BibleScreen
import app.jwhabits.ui.service.ServiceScreen
import app.jwhabits.ui.settings.SettingsScreen
import app.jwhabits.ui.study.ShareReceiveDialog
import app.jwhabits.ui.study.StudyScreen
import app.jwhabits.ui.study.StudyViewModel
import app.jwhabits.ui.today.TodayScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("今日", Icons.Filled.Home),
    BIBLE("聖書通読", Icons.Filled.Menu),
    STUDY("個人研究", Icons.Filled.Person),
    SERVICE("奉仕", Icons.Filled.DateRange),
}

/** 記録のあとに「元に戻す」を出す。入力画面は出さず、誤タップはこれで戻す。 */
class Undoer(private val host: SnackbarHostState, private val scope: CoroutineScope) {
    /** 「元に戻す」なしの短いお知らせ。 */
    fun message(text: String) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            host.showSnackbar(text, duration = SnackbarDuration.Short)
        }
    }

    fun offer(message: String, undo: suspend () -> Unit) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            val r = host.showSnackbar(message, actionLabel = "元に戻す", duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) undo()
        }
    }
}

val LocalUndoer = staticCompositionLocalOf<Undoer> { error("Undoer not provided") }

@Composable
fun AppRoot(
    pendingShare: MutableState<SharedPayload?> = remember { mutableStateOf(null) },
    pendingTab: MutableState<Tab?> = remember { mutableStateOf(null) },
) {
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    pendingTab.value?.let { requested ->
        LaunchedEffect(requested) {
            tab = requested
            pendingTab.value = null
        }
    }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undoer = remember { Undoer(snackbar, scope) }

    CompositionLocalProvider(LocalUndoer provides undoer) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t; showSettings = false },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label) },
                        )
                    }
                }
            },
        ) { padding ->
            androidx.compose.foundation.layout.Column(Modifier.padding(padding).fillMaxSize()) {
                // 新しい版があれば、いちばん上に知らせ（設定を開いているときは設定の中に出す）
                if (!showSettings) app.jwhabits.update.UpdateBanner(Modifier.padding(8.dp))
            Box(Modifier.weight(1f).fillMaxSize()) {
                if (showSettings) {
                    SettingsScreen(onBack = { showSettings = false })
                } else {
                    when (tab) {
                        Tab.TODAY -> TodayScreen(
                            onOpenBible = { tab = Tab.BIBLE },
                            onOpenStudy = { tab = Tab.STUDY },
                            onOpenSettings = { showSettings = true },
                        )
                        Tab.BIBLE -> BibleScreen()
                        Tab.STUDY -> StudyScreen()
                        Tab.SERVICE -> ServiceScreen()
                    }
                }
            }
            }
        }
        pendingShare.value?.let { payload ->
            val study: StudyViewModel = viewModel(factory = StudyViewModel.Factory)
            ShareReceiveDialog(payload, study) { saved ->
                pendingShare.value = null
                if (saved) tab = Tab.STUDY
            }
        }
    }
}

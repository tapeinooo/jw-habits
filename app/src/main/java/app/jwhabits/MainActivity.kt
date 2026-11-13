package app.jwhabits

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import app.jwhabits.domain.SharedPayload
import app.jwhabits.domain.ShareParser
import app.jwhabits.ui.AppRoot
import app.jwhabits.ui.JwHabitsTheme
import app.jwhabits.ui.Tab
import app.jwhabits.widget.WidgetIntents

class MainActivity : ComponentActivity() {
    /** 「共有」で受け取った内容。記録先を選び終わるまで持つ。 */
    private val pendingShare = mutableStateOf<SharedPayload?>(null)

    /** ウィジェットから「このタブで開いて」と頼まれたとき。開いたら null に戻す。 */
    private val pendingTab = mutableStateOf<Tab?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 画面の回転などで作り直されたときは、同じ共有をもう一度出さない
        if (savedInstanceState == null) {
            handleShare(intent)
            handleTab(intent)
        }
        setContent { JwHabitsTheme { AppRoot(pendingShare, pendingTab) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShare(intent)
        handleTab(intent)
    }

    private fun handleTab(intent: Intent?) {
        val name = intent?.getStringExtra(WidgetIntents.EXTRA_TAB) ?: return
        pendingTab.value = Tab.entries.firstOrNull { it.name == name }
        intent.removeExtra(WidgetIntents.EXTRA_TAB)
    }

    private fun handleShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type?.startsWith("text/plain") != true) return
        ShareParser.parse(intent.getStringExtra(Intent.EXTRA_TEXT), intent.getStringExtra(Intent.EXTRA_SUBJECT))
            ?.let { pendingShare.value = it }
        intent.action = null // 同じ共有を二度処理しない
    }
}

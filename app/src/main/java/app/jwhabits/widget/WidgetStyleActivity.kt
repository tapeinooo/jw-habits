package app.jwhabits.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.jwhabits.ui.JwHabitsTheme
import kotlinx.coroutines.launch

/** ウィジェットの設定画面：タイルごとの背景の濃さ。ウィジェットを置くときと、長押しの「設定」から開く。 */
class WidgetStyleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 戻る操作でもウィジェットは置かれるように、先に OK を返しておく
        val id = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        }
        setContent { JwHabitsTheme { Screen(onDone = { finish() }) } }
    }

    @Composable
    private fun Screen(onDone: () -> Unit) {
        val scope = rememberCoroutineScope()
        val app = applicationContext
        val values = remember { WidgetStyle.Tile.entries.associateWith { mutableIntStateOf(WidgetStyle.get(app, it)) } }
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("ウィジェットの背景", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("左が透明、右が濃い。", color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF5B7A99)).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Preview(Modifier.weight(1f), Color(0xFFC9CED6), Color(0xFF17202B), values.getValue(WidgetStyle.Tile.BIBLE).intValue, "創世記", "4")
                Preview(Modifier.weight(1f), Color(0xFF1C2E4A), Color.White, values.getValue(WidgetStyle.Tile.STUDY).intValue, "ミカ書", "")
                Preview(Modifier.weight(1f), Color.Black, Color.White, values.getValue(WidgetStyle.Tile.SERVICE).intValue, "52:30", "")
            }

            Row3("聖書通読", WidgetStyle.Tile.BIBLE, values) { t, v -> WidgetStyle.set(app, t, v) }
            Row3("個人研究", WidgetStyle.Tile.STUDY, values) { t, v -> WidgetStyle.set(app, t, v) }
            Row3("奉仕時間", WidgetStyle.Tile.SERVICE, values) { t, v -> WidgetStyle.set(app, t, v) }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { scope.launch { WidgetUpdater.updateAll(app); onDone() } },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("完了") }
        }
    }

    @Composable
    private fun Row3(
        label: String,
        tile: WidgetStyle.Tile,
        values: Map<WidgetStyle.Tile, androidx.compose.runtime.MutableIntState>,
        save: (WidgetStyle.Tile, Int) -> Unit,
    ) {
        var v by values.getValue(tile)
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, fontWeight = FontWeight.Bold)
                Text("$v%")
            }
            Slider(
                value = v.toFloat(), onValueChange = { v = it.toInt(); save(tile, v) },
                valueRange = 0f..100f,
            )
        }
    }

    @Composable
    private fun Preview(modifier: Modifier, bg: Color, ink: Color, percent: Int, small: String, big: String) {
        Box(
            modifier.height(72.dp).clip(RoundedCornerShape(14.dp)).background(bg.copy(alpha = percent / 100f)),
            contentAlignment = Alignment.CenterStart,
        ) {
            Column(Modifier.padding(horizontal = 10.dp)) {
                // 透明に近いときは壁紙の上に白で見える想定
                val c = if (percent < 40 && bg != Color.Black) Color.White else if (bg == Color.Black) Color.White else ink
                Text(small, color = c, fontSize = if (big.isEmpty()) 18.sp else 12.sp, fontWeight = FontWeight.Bold)
                if (big.isNotEmpty()) Text(big, color = c, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

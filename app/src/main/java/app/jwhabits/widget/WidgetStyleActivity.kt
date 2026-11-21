package app.jwhabits.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import app.jwhabits.ui.JwHabitsTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** ウィジェット「今日の3つ」の設定：タイルごとの背景の濃さ。置くときと、長押しの鉛筆ボタンから開く。 */
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
        setContent { JwHabitsTheme { StyleScreen(onClose = { finish() }) } }
    }

    /** 戻る操作で閉じても反映されるように、画面を離れるときに描き直す。 */
    override fun onPause() {
        super.onPause()
        val app = applicationContext
        lifecycleScope.launch { WidgetUpdater.updateAll(app) }
    }

    private data class TileSpec(val tile: WidgetStyle.Tile, val name: String, val color: Color, val ink: Color)

    private val specs = listOf(
        TileSpec(WidgetStyle.Tile.BIBLE, "聖書通読", Color(0xFFC9CED6), Color(0xFF17202B)),
        TileSpec(WidgetStyle.Tile.STUDY, "個人研究", Color(0xFF1C2E4A), Color.White),
        TileSpec(WidgetStyle.Tile.SERVICE, "奉仕時間", Color.Black, Color.White),
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun StyleScreen(onClose: () -> Unit) {
        val app = applicationContext
        val values = remember { specs.associate { it.tile to mutableIntStateOf(WidgetStyle.get(app, it.tile)) } }
        var previewDone by remember { mutableStateOf(false) }
        val textScale = remember { mutableIntStateOf(WidgetStyle.textScale(app)) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("ウィジェットの見た目") },
                    navigationIcon = {
                        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "閉じる") }
                    },
                )
            },
            bottomBar = {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp).height(52.dp),
                    ) { Text("完了", fontSize = 16.sp) }
                }
            },
        ) { inner ->
            Column(
                Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionTitle("プレビュー")
                Preview(values, previewDone, textScale.intValue / 100f)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("まだ" to false, "済んだ" to true).forEachIndexed { i, (label, done) ->
                        SegmentedButton(
                            selected = previewDone == done,
                            onClick = { previewDone = done },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                        ) { Text(label) }
                    }
                }

                SectionTitle("背景の濃さ", top = 12)
                Text(
                    "背景だけが透けます。文字はそのまま。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                specs.forEach { spec ->
                    OpacityCard(spec, values.getValue(spec.tile)) { WidgetStyle.set(app, spec.tile, it) }
                }

                SectionTitle("文字の大きさ", top = 12)
                Text(
                    "端末のフォントサイズの設定には左右されない、固定の大きさです。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextSizeCard(textScale) { WidgetStyle.setTextScale(app, it) }
            }
        }
    }

    @Composable
    private fun SectionTitle(text: String, top: Int = 0) {
        Text(
            text, modifier = Modifier.padding(top = top.dp),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    @Composable
    private fun OpacityCard(spec: TileSpec, state: MutableIntState, save: (Int) -> Unit) {
        var v by state
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(22.dp).clip(CircleShape).background(spec.color)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    )
                    Text(
                        spec.name, modifier = Modifier.padding(start = 10.dp).weight(1f),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    )
                    Text("$v%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = v.toFloat(),
                    onValueChange = { v = (it / 5f).roundToInt() * 5; save(v) },
                    valueRange = 0f..100f,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Hint("透明")
                    Hint("濃い")
                }
            }
        }
    }

    @Composable
    private fun TextSizeCard(state: MutableIntState, save: (Int) -> Unit) {
        var v by state
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "すべての文字", modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    )
                    Text("$v%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = v.toFloat(),
                    onValueChange = { v = (it / 5f).roundToInt() * 5; save(v) },
                    valueRange = WidgetStyle.TEXT_MIN.toFloat()..WidgetStyle.TEXT_MAX.toFloat(),
                )
                Row(
                    Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Hint("小さい")
                    TextButton(onClick = { v = 100; save(100) }) { Text("100%に戻す") }
                    Hint("大きい")
                }
            }
        }
    }

    @Composable
    private fun Hint(text: String) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    /** ホーム画面の上に置いたときの見え方（壁紙の代わりに濃い色のグラデーション）。 */
    @Composable
    private fun Preview(values: Map<WidgetStyle.Tile, MutableIntState>, done: Boolean, scale: Float) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF3B5578), Color(0xFF141B2B))))
                .padding(horizontal = 10.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val (bible, study, service) = specs
            PreviewTile(Modifier.weight(1f), bible, values.getValue(bible.tile).intValue, "創世記", "4", done, scale)
            PreviewTile(Modifier.weight(1f), study, values.getValue(study.tile).intValue, "ミカ書", "", done, scale)
            PreviewTile(Modifier.weight(1f), service, values.getValue(service.tile).intValue, "52:30", "3件", null, scale)
        }
    }

    /** done = null は済んだ／まだの印を出さない（奉仕）。文字はウィジェットと同じ dp の大きさ。 */
    @Composable
    private fun PreviewTile(
        modifier: Modifier, spec: TileSpec, percent: Int, small: String, big: String, done: Boolean?, scale: Float,
    ) {
        val density = LocalDensity.current
        fun sz(n: Int) = with(density) { (n * scale).dp.toSp() }
        val alpha = percent / 100f
        val ink = if (alpha < 0.4f) Color.White else spec.ink
        Box(modifier.height(96.dp).clip(RoundedCornerShape(16.dp)).background(spec.color.copy(alpha = alpha))) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = if (done == null) Alignment.CenterHorizontally else Alignment.Start,
            ) {
                when {
                    done == null -> {
                        Text(small, color = ink, fontSize = sz(30), fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(big, color = ink, fontSize = sz(18), fontWeight = FontWeight.Bold)
                    }
                    big.isNotEmpty() -> {
                        Text(small, color = ink, fontSize = sz(16), fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(big, color = ink, fontSize = sz(32), fontWeight = FontWeight.Bold)
                    }
                    else -> Text(small, color = ink, fontSize = sz(24), maxLines = 2, fontWeight = FontWeight.Bold)
                }
            }
            if (done != null) {
                Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.TopEnd) {
                    if (done) Text("✓", color = ink, fontSize = sz(22), fontWeight = FontWeight.Bold)
                    else Box(Modifier.size((10 * scale).dp).clip(CircleShape).background(Color(0xFFFF4D4F)))
                }
            }
        }
    }
}

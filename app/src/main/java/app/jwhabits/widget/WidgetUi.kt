package app.jwhabits.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxWidth

/** 4つのウィジェットに共通の背景・文字。 */
@Composable
fun WidgetFrame(modifier: GlanceModifier = GlanceModifier, content: @Composable RowScope.() -> Unit) {
    GlanceTheme {
        Row(
            modifier = GlanceModifier.fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(20.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .then(modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) { content() }
    }
}

@Composable
fun WText(
    text: String,
    size: Int = 16,
    bold: Boolean = false,
    muted: Boolean = false,
    color: ColorProvider? = null,
    modifier: GlanceModifier = GlanceModifier,
    maxLines: Int = 1,
) {
    Text(
        text,
        modifier = modifier,
        maxLines = maxLines,
        style = TextStyle(
            color = color ?: if (muted) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface,
            fontSize = size.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        ),
    )
}

/** 「記録しました ・ 元に戻す」（数秒だけ出す）。 */
@Composable
fun UndoLine(modifier: GlanceModifier = GlanceModifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        WText("記録しました", modifier = GlanceModifier.defaultWeight())
        Button("元に戻す", onClick = actionRunCallback<UndoAction>())
    }
}

/** 次の単位の行：名前（タップでリンク）と ✓ ボタン。 */
@Composable
fun RowScope.NextUnitWidgetRow(
    caption: String,
    label: String,
    onClickLabel: Action,
    onCheck: Action?,
) {
    Column(modifier = GlanceModifier.defaultWeight().clickable(onClickLabel)) {
        WText(caption, size = 11, muted = true)
        WText(label, size = 20, bold = true)
    }
    if (onCheck != null) Button("✓", onClick = onCheck)
}

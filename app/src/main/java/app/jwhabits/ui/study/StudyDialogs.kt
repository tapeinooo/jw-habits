package app.jwhabits.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.jwhabits.data.MaterialEntity
import app.jwhabits.data.MaterialTemplate
import app.jwhabits.domain.StudyTemplates
import java.time.LocalDate

private val numberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number)

@Composable
fun AddMaterialDialog(
    onDismiss: () -> Unit,
    onBibleStudy: () -> Unit,
    onBe: () -> Unit,
    onWatchtower: () -> Unit,
    onManual: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("資料を追加") },
        text = {
            Column {
                TextButton(onClick = onBibleStudy, modifier = Modifier.fillMaxWidth()) { Text(app.jwhabits.domain.SiContents.TITLE) }
                TextButton(onClick = onBe, modifier = Modifier.fillMaxWidth()) { Text("『神権宣教学校の教育から益を得る』") }
                TextButton(onClick = onWatchtower, modifier = Modifier.fillMaxWidth()) { Text("ものみの塔（研究用）") }
                TextButton(onClick = onManual, modifier = Modifier.fillMaxWidth()) { Text("手で作る") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("やめる") } },
    )
}

@Composable
fun WatchtowerIssueDialog(default: LocalDate, onDismiss: () -> Unit, onCreate: (year: Int, month: Int) -> Unit) {
    var year by remember { mutableStateOf(default.year.toString()) }
    var month by remember { mutableStateOf(default.monthValue.toString()) }
    val y = year.toIntOrNull()?.takeIf { it in 1900..2999 }
    val m = month.toIntOrNull()?.takeIf { it in 1..12 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("何年何月号ですか") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, label = { Text("年") }, singleLine = true,
                    keyboardOptions = numberKeyboard, modifier = Modifier.weight(1f))
                OutlinedTextField(month, { month = it.filter(Char::isDigit).take(2) }, label = { Text("月") }, singleLine = true,
                    keyboardOptions = numberKeyboard, modifier = Modifier.weight(1f))
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(y!!, m!!) }, enabled = y != null && m != null) { Text("追加") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("やめる") } },
    )
}

/** 手で作る資料：名前・単位の呼び名・数（空なら「記録だけの資料」）。 */
@Composable
fun ManualMaterialDialog(
    onDismiss: () -> Unit,
    onCreate: (StudyTemplates.Spec) -> Unit,
    initialTitle: String = "",
) {
    var title by remember { mutableStateOf(initialTitle) }
    var label by remember { mutableStateOf("章") }
    var count by remember { mutableStateOf("") }
    val n = count.toIntOrNull()?.takeIf { it > 0 }
    val valid = title.isNotBlank() && label.isNotBlank() && (count.isEmpty() || n != null)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("手で作る") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("名前") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(label, { label = it }, label = { Text("単位の呼び名（課・章・研究・記事）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(count, { count = it.filter(Char::isDigit).take(4) }, label = { Text("数（空なら記録だけの資料）") },
                    singleLine = true, keyboardOptions = numberKeyboard, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(StudyTemplates.manual(title.trim(), label.trim(), n)) }, enabled = valid) { Text("作る") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("やめる") } },
    )
}

/** 名前・数を変える。聖書全体は数を変えられない。数のある資料を「記録だけ」には戻せない。 */
@Composable
fun EditMaterialDialog(material: MaterialEntity, onDismiss: () -> Unit, onSave: (MaterialEntity) -> Unit) {
    val isBible = material.template == MaterialTemplate.BIBLE_STUDY || material.template == MaterialTemplate.SI
    val manual = material.template == null
    var title by remember { mutableStateOf(material.title) }
    var label by remember { mutableStateOf(material.unitLabel) }
    var count by remember { mutableStateOf(material.unitCount?.toString().orEmpty()) }
    val n = count.toIntOrNull()?.takeIf { it > 0 }
    val countValid = isBible || n != null || (count.isEmpty() && material.unitCount == null)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("名前・数を変える") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("名前") }, modifier = Modifier.fillMaxWidth())
                if (manual) {
                    OutlinedTextField(label, { label = it }, label = { Text("単位の呼び名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                if (!isBible) {
                    OutlinedTextField(count, { count = it.filter(Char::isDigit).take(4) },
                        label = { Text(if (material.unitCount == null) "数（空なら記録だけの資料）" else "数（${material.unitLabel}）") },
                        singleLine = true, keyboardOptions = numberKeyboard, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(material.copy(
                        title = title.trim(),
                        unitLabel = if (manual) label.trim() else material.unitLabel,
                        unitCount = if (isBible) material.unitCount else n,
                    ))
                },
                enabled = title.isNotBlank() && label.isNotBlank() && countValid,
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("やめる") } },
    )
}

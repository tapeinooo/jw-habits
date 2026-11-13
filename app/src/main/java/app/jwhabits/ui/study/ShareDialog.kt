package app.jwhabits.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jwhabits.data.MaterialEntity
import app.jwhabits.data.MaterialTemplate
import app.jwhabits.domain.BibleBooks
import app.jwhabits.domain.SharedPayload
import app.jwhabits.domain.StudyTemplates
import app.jwhabits.ui.LocalUndoer

/**
 * JW Library から「共有」で受け取ったリンクを、資料に結びつける全画面の画面。
 * 共有の形式は未確認なので、題名と記録先（資料・単位）は手で選ぶ。
 */
@Composable
fun ShareReceiveDialog(payload: SharedPayload, study: StudyViewModel, onClose: (saved: Boolean) -> Unit) {
    val undoer = LocalUndoer.current
    val materials by study.materials.collectAsStateWithLifecycle()
    val currentId by study.currentId.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf(payload.title) }
    var createNew by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var unitIndex by remember { mutableStateOf<Int?>(null) }
    var bookNumber by remember { mutableStateOf(1) }
    var newLabel by remember { mutableStateOf("章") }
    var newCount by remember { mutableStateOf("") }

    // 既定の選択：同じ号のものみの塔 → 今使っている資料 → 先頭
    val defaultId = materials.firstOrNull { payload.issue != null && it.material.issue == payload.issue }?.material?.id
        ?: materials.firstOrNull { it.material.id == currentId }?.material?.id
        ?: materials.firstOrNull()?.material?.id
    val chosen = materials.firstOrNull { it.material.id == (selectedId ?: defaultId) }?.material
    val newMode = createNew || materials.isEmpty()
    val count = newCount.toIntOrNull()?.takeIf { it > 0 }
    val canSave = if (newMode) title.isNotBlank() && newLabel.isNotBlank() && (newCount.isEmpty() || count != null) else chosen != null

    fun save() {
        val link = payload.link
        if (newMode) {
            study.create(StudyTemplates.manual(title.trim(), newLabel.trim(), count)) { id ->
                study.saveShare(id, null, link, undoer::offer)
            }
        } else {
            chosen ?: return
            study.saveShare(chosen.id, unitIndex, link, undoer::offer)
        }
        onClose(true)
    }

    // Dialog の窓は画面の下にはみ出すため、アプリの上に全画面で重ねる
    BackHandler { onClose(false) }
    run {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("共有を受け取る", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { onClose(false) }) { Text("やめる") }
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(52.dp),
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    full { OutlinedTextField(title, { title = it }, label = { Text("題名") }, modifier = Modifier.fillMaxWidth()) }
                    full {
                        OutlinedTextField(
                            payload.link ?: "（リンクはありません）", {}, readOnly = true,
                            label = { Text("リンク") }, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    full {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !newMode, onClick = { createNew = false }, enabled = materials.isNotEmpty(),
                                label = { Text("今ある資料に記録") })
                            FilterChip(selected = newMode, onClick = { createNew = true }, label = { Text("新しい資料として登録") })
                        }
                    }
                    if (newMode) {
                        full {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(newLabel, { newLabel = it }, label = { Text("単位の呼び名（課・章・研究・記事）") },
                                    singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(newCount, { newCount = it.filter(Char::isDigit).take(4) },
                                    label = { Text("数（空なら記録だけの資料）") }, singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                            }
                        }
                    } else {
                        items(materials, key = { it.material.id }, span = { GridItemSpan(maxLineSpan) }) { item ->
                            val selected = item.material.id == chosen?.id
                            Card(
                                Modifier.fillMaxWidth().clickable { selectedId = item.material.id; unitIndex = null },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            ) {
                                Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected, onClick = { selectedId = item.material.id; unitIndex = null })
                                    Text(item.material.title, Modifier.weight(1f).padding(vertical = 8.dp))
                                }
                            }
                        }
                        if (chosen != null) unitPicker(chosen, unitIndex, bookNumber, { bookNumber = it }, { unitIndex = it })
                    }
                }
                Button(
                    onClick = ::save, enabled = canSave,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) { Text("保存") }
            }
        }
    }
}

private fun LazyGridScope.full(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

/** 記録する単位を選ぶ（選ばなくてもよい）。聖書は書を選んでから章。 */
private fun LazyGridScope.unitPicker(
    material: MaterialEntity,
    selected: Int?,
    bookNumber: Int,
    onBook: (Int) -> Unit,
    onUnit: (Int?) -> Unit,
) {
    val count = material.unitCount
    if (count == null) {
        full { Text("記録を1件追加します。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        return
    }
    val isBible = material.template == MaterialTemplate.BIBLE_STUDY
    full {
        Text(
            "どの${material.unitLabel}ですか（選ばなければ資料にリンクだけ保存）",
            style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp),
        )
    }
    if (material.template == MaterialTemplate.SI) {
        // 見出しの一覧から1つ選ぶ
        items(count, key = { "si-$it" }, span = { GridItemSpan(maxLineSpan) }) { index ->
            val on = selected == index
            val scheme = MaterialTheme.colorScheme
            Text(
                app.jwhabits.domain.SiContents.heading(index),
                color = if (on) scheme.onPrimary else scheme.onSurface,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(if (on) scheme.primary else scheme.surfaceVariant)
                    .clickable { onUnit(if (on) null else index) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
        return
    }
    val base: Int
    val cells: Int
    if (isBible) {
        val book = BibleBooks.book(bookNumber)
        full { BookDropdown(book.name, onBook) }
        base = book.firstIndex
        cells = book.chapters
    } else {
        base = 0
        cells = count
    }
    items((1..cells).toList(), key = { "cell-$bookNumber-$it" }) { n ->
        val index = base + n - 1
        val on = selected == index
        val scheme = MaterialTheme.colorScheme
        Box(
            Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                .background(if (on) scheme.primary else scheme.surfaceVariant)
                .clickable { onUnit(if (on) null else index) },
            contentAlignment = Alignment.Center,
        ) { Text("$n", color = if (on) scheme.onPrimary else scheme.onSurfaceVariant) }
    }
}

@Composable
private fun BookDropdown(name: String, onBook: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text(name) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            BibleBooks.all.forEach { b ->
                DropdownMenuItem(text = { Text(b.name) }, onClick = { open = false; onBook(b.number) })
            }
        }
    }
}

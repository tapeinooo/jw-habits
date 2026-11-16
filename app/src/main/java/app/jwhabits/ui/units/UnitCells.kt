package app.jwhabits.ui.units

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.jwhabits.domain.JwLibrary
import app.jwhabits.domain.ReadingProgress
import app.jwhabits.ui.LocalUndoer
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

enum class GridMode { NORMAL, RANGE, DATE }

/** リンクを JW Library（またはブラウザ）で開く。なければ「リンクがありません」。 */
fun openLink(context: android.content.Context, link: String?) {
    if (link == null) Toast.makeText(context, "リンクがありません", Toast.LENGTH_SHORT).show()
    else JwLibrary.open(context, link)
}

/** 「次：研究 12 ✓」。✓で記録、名前タップ／長押しは呼び出し側が決める。caption は資料名など。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NextUnitRow(
    label: String,
    onCheck: () -> Unit,
    onClickLabel: (() -> Unit)?,
    onLongPressLabel: (() -> Unit)?,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.weight(1f).combinedClickable(
                    onClick = { onClickLabel?.invoke() },
                    onLongClick = { onLongPressLabel?.invoke() },
                ),
            ) {
                if (caption != null) {
                    Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Text(label, style = MaterialTheme.typography.titleMedium)
            }
            FilledTonalIconButton(onClick = onCheck) {
                Icon(Icons.Filled.Check, contentDescription = "$label を記録")
            }
        }
    }
}

/**
 * 状態から「次」の行を出す（聖書も研究の資料も）。数を決めない資料は「今日記録」。
 * onClickLabel は名前タップの動き（なければ何もしない）。
 */
@Composable
fun NextRowFor(
    state: UnitUiState,
    vm: UnitViewModel,
    modifier: Modifier = Modifier,
    showTitle: Boolean = false,
    onClickLabel: ((Int) -> Unit)? = null,
) {
    val material = state.material ?: return
    val undoer = LocalUndoer.current
    val context = LocalContext.current
    val caption = if (showTitle) material.title else null
    if (state.countless) {
        NextUnitRow(
            label = "今日記録", caption = caption, modifier = modifier,
            onCheck = { vm.addLog(undoer::offer) }, onClickLabel = null,
            onLongPressLabel = { openLink(context, state.linkFor(null)) },
        )
        return
    }
    val next = state.nextIndex ?: return
    if (state.allDone) return
    NextUnitRow(
        label = "次：${state.nameOf(next)}", caption = caption, modifier = modifier,
        onCheck = { vm.toggle(next, undoer::offer) },
        onClickLabel = onClickLabel?.let { f -> { f(next) } },
        onLongPressLabel = { openLink(context, state.linkFor(next)) },
    )
}

/** 画面上部のバー。menu にメニュー項目を入れる（close でメニューを閉じる）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitTopBar(
    title: String,
    backDescription: String,
    onBack: () -> Unit,
    menu: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    TopAppBar(
        windowInsets = WindowInsets(0), // 外側の Scaffold がすでに余白を取っている
        title = { Text(title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, backDescription) } },
        actions = {
            IconButton(onClick = { open = true }) { Icon(Icons.Filled.MoreVert, "メニュー") }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) { menu { open = false } }
        },
    )
}

@Composable
fun ColumnScope.ModeMenuItems(onMode: (GridMode) -> Unit, close: () -> Unit) {
    DropdownMenuItem(text = { Text("なぞってまとめて記録") }, onClick = { close(); onMode(GridMode.RANGE) })
    DropdownMenuItem(text = { Text("読んだ日を直す") }, onClick = { close(); onMode(GridMode.DATE) })
}

@Composable
fun ModeBanner(mode: GridMode, unitLabel: String, onDone: () -> Unit) {
    val text = when (mode) {
        GridMode.RANGE -> "記録したい${unitLabel}を指でなぞってください"
        GridMode.DATE -> "日付を直す${unitLabel}をタップしてください"
        GridMode.NORMAL -> return
    }
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onDone) { Text("完了") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitDatePickerDialog(title: String, initial: LocalDate, today: LocalDate, onSave: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = initial.toEpochDay() * 86_400_000L,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= today.toEpochDay() * 86_400_000L
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                picker.selectedDateMillis?.let { onSave(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("やめる") } },
    ) {
        DatePicker(picker, title = { Text(title, Modifier.padding(start = 24.dp, top = 16.dp)) })
    }
}

/** 番号 1..n のマス目で記録する資料（研究・記事・手で作った数ありの資料）の画面。 */
@Composable
fun UnitGrid(
    state: UnitUiState,
    vm: UnitViewModel,
    title: String,
    onBack: () -> Unit,
    menuExtras: @Composable ColumnScope.(close: () -> Unit) -> Unit,
    /** true なら見出しを1行ずつ並べる（マス目ではなく一覧。なぞって記録はない）。 */
    rows: Boolean = false,
) {
    val unit = state.material?.unitLabel.orEmpty()
    var mode by rememberSaveable { mutableStateOf(GridMode.NORMAL) }
    var dateTarget by remember { mutableStateOf<Int?>(null) }
    var confirmNextRound by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        UnitTopBar(title, "資料の一覧へ", onBack) { close ->
            if (rows) {
                DropdownMenuItem(text = { Text("読んだ日を直す") }, onClick = { close(); mode = GridMode.DATE })
            } else {
                ModeMenuItems({ mode = it }, close)
            }
            menuExtras(close)
        }
        Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "${state.round}周目 ・ ${state.readCount} / ${state.total}$unit" +
                    if (state.todayCount > 0) " ・ 今日 ${state.todayCount}$unit" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.allDone) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("全${state.total}$unit を終えました", style = MaterialTheme.typography.titleMedium)
                        Button(onClick = { confirmNextRound = true }) { Text("${state.round + 1}周目を始める") }
                    }
                }
            } else if (mode == GridMode.NORMAL) {
                NextRowFor(state, vm)
            }
        }
        if (mode != GridMode.NORMAL) ModeBanner(mode, unit) { mode = GridMode.NORMAL }
        Spacer(Modifier.height(8.dp))
        if (rows) {
            UnitRows(state, mode, vm, onPickDate = { dateTarget = it }, modifier = Modifier.weight(1f))
        } else {
            UnitCells(
                count = state.total,
                state = state,
                mode = mode,
                vm = vm,
                indexOf = { it - 1 },
                onPickDate = { dateTarget = it },
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
        }
    }

    dateTarget?.let { index ->
        UnitDatePickerDialog(
            title = state.nameOf(index),
            initial = state.read[index]?.date ?: vm.today(),
            today = vm.today(),
            onSave = { vm.setDate(index, it) },
            onDismiss = { dateTarget = null },
        )
    }
    if (confirmNextRound) NextRoundDialog(state.round, { confirmNextRound = false }) { vm.startNextRound() }
}

@Composable
fun NextRoundDialog(round: Int, onDismiss: () -> Unit, onStart: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${round + 1}周目を始めますか") },
        text = { Text("${round}周目の記録は履歴に残ります。") },
        confirmButton = { TextButton(onClick = { onDismiss(); onStart() }) { Text("始める") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("やめる") } },
    )
}

/**
 * 番号のマス目（1..count）。indexOf は「マスの番号 → 単位の番号（0始まり）」。
 * 通常：タップで記録／取り消し、長押しでリンク。
 * なぞる：指を置いたマスから今のマスまでを範囲として塗り、離すと記録する（端に来たら自動でスクロール）。
 * 日付：タップで日付を選ぶ。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UnitCells(
    count: Int,
    state: UnitUiState,
    mode: GridMode,
    vm: UnitViewModel,
    indexOf: (Int) -> Int,
    onPickDate: (Int) -> Unit,
    modifier: Modifier,
    labelOf: (Int) -> String = { "$it" },
    onLongPress: ((Int) -> Unit)? = null,
) {
    val undoer = LocalUndoer.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val gridState = rememberLazyGridState()
    val today = state.today
    // 開いたときに1度だけ、次の単位のマスを中央あたりに
    var centered by remember { mutableStateOf(false) }
    LaunchedEffect(state.loaded, state.nextIndex) {
        if (centered || !state.loaded) return@LaunchedEffect
        centered = true
        val next = state.nextIndex ?: return@LaunchedEffect
        val cell = (1..count).firstOrNull { indexOf(it) == next } ?: return@LaunchedEffect
        centerGridItem(gridState, cell - 1)
    }

    // なぞっている範囲（マスの番号 1始まり）
    var dragStart by remember { mutableStateOf<Int?>(null) }
    var dragEnd by remember { mutableStateOf<Int?>(null) }
    var pointer by remember { mutableStateOf(Offset.Zero) }
    var autoScroll by remember { mutableFloatStateOf(0f) }
    val edge = with(LocalDensity.current) { 56.dp.toPx() }
    val painted = dragStart?.let { s -> dragEnd?.let { e -> ReadingProgress.range(s, e) } }

    LaunchedEffect(autoScroll) {
        while (autoScroll != 0f) {
            gridState.scrollBy(autoScroll)
            cellAt(gridState, pointer)?.let { dragEnd = it }
            delay(16)
        }
    }

    val dragModifier = if (mode == GridMode.RANGE) {
        Modifier.pointerInput(count) {
            detectDragGestures(
                onDragStart = { p ->
                    pointer = p
                    dragStart = cellAt(gridState, p)
                    dragEnd = dragStart
                    if (dragStart != null) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDrag = { change, _ ->
                    pointer = change.position
                    cellAt(gridState, pointer)?.let { dragEnd = it }
                    autoScroll = when {
                        pointer.y < edge -> -12f
                        pointer.y > size.height - edge -> 12f
                        else -> 0f
                    }
                },
                onDragEnd = {
                    autoScroll = 0f
                    val s = dragStart
                    val e = dragEnd
                    if (s != null && e != null) {
                        vm.markRange(ReadingProgress.range(s, e).map(indexOf), undoer::offer)
                    }
                    dragStart = null; dragEnd = null
                },
                onDragCancel = { autoScroll = 0f; dragStart = null; dragEnd = null },
            )
        }
    } else Modifier

    LazyVerticalGrid(
        columns = GridCells.Adaptive(56.dp),
        state = gridState,
        userScrollEnabled = mode != GridMode.RANGE,
        modifier = modifier.then(dragModifier),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items((1..count).toList(), key = { it }) { cell ->
            val index = indexOf(cell)
            val record = state.read[index]
            val isPainted = painted != null && cell in painted
            val scheme = MaterialTheme.colorScheme
            val bg = when {
                isPainted -> scheme.tertiary
                record != null -> scheme.primary
                else -> scheme.surfaceVariant
            }
            val fg = when {
                isPainted -> scheme.onTertiary
                record != null -> scheme.onPrimary
                else -> scheme.onSurfaceVariant
            }
            val clickMod = when (mode) {
                GridMode.NORMAL -> Modifier.combinedClickable(
                    onClick = { vm.toggle(index, undoer::offer) },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress?.invoke(cell) ?: openLink(context, state.linkFor(index))
                    },
                )
                GridMode.DATE -> Modifier.combinedClickable(onClick = { onPickDate(index) })
                GridMode.RANGE -> Modifier
            }
            Box(
                Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(bg).then(clickMod),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(labelOf(cell), color = fg, fontWeight = if (record != null) FontWeight.Bold else FontWeight.Normal)
                    if (record != null && !isPainted) {
                        Text(shortDate(record.date, today), color = fg.copy(alpha = 0.8f), fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

/** 指の位置にあるマスの番号（1始まり）。 */
private fun cellAt(state: LazyGridState, p: Offset): Int? =
    state.layoutInfo.visibleItemsInfo.firstOrNull { item ->
        p.x >= item.offset.x && p.x < item.offset.x + item.size.width &&
            p.y >= item.offset.y && p.y < item.offset.y + item.size.height
    }?.key as? Int

fun shortDate(d: LocalDate, today: LocalDate): String =
    if (d.year == today.year) "${d.monthValue}/${d.dayOfMonth}" else "${d.year % 100}/${d.monthValue}/${d.dayOfMonth}"

/**
 * 見出しの一覧（1行に1つ）。通常：タップで記録／取り消し、長押しでその見出しのページ。日付：タップで日付を選ぶ。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UnitRows(
    state: UnitUiState,
    mode: GridMode,
    vm: UnitViewModel,
    onPickDate: (Int) -> Unit,
    modifier: Modifier,
) {
    val undoer = LocalUndoer.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val today = vm.today()
    val scheme = MaterialTheme.colorScheme
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    var centered by remember { mutableStateOf(false) }
    LaunchedEffect(state.loaded, state.nextIndex) {
        if (centered || !state.loaded) return@LaunchedEffect
        centered = true
        val next = state.nextIndex ?: return@LaunchedEffect
        listState.scrollToItem(next)
        androidx.compose.runtime.withFrameNanos { }
        val info = listState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == next } ?: return@LaunchedEffect
        val viewport = info.viewportEndOffset - info.viewportStartOffset
        listState.scrollBy((item.offset - (viewport - item.size) / 2).toFloat())
    }
    androidx.compose.foundation.lazy.LazyColumn(modifier, state = listState) {
        items(state.total, key = { it }) { index ->
            val record = state.read[index]
            val click = when (mode) {
                GridMode.DATE -> Modifier.combinedClickable(onClick = { onPickDate(index) })
                else -> Modifier.combinedClickable(
                    onClick = { vm.toggle(index, undoer::offer) },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        openLink(context, state.linkFor(index))
                    },
                )
            }
            Row(
                Modifier.fillMaxWidth().then(click).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (record != null) scheme.primary else scheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (record != null) Icon(Icons.Filled.Check, null, tint = scheme.onPrimary, modifier = Modifier.size(16.dp))
                }
                Text(
                    state.nameOf(index),
                    Modifier.weight(1f).padding(start = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (record != null) scheme.onSurfaceVariant else scheme.onSurface,
                )
                if (record != null) {
                    Text(shortDate(record.date, today), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** グリッドの index 番目の項目を、画面の中央あたりに来るようにスクロールする。 */
suspend fun centerGridItem(gridState: androidx.compose.foundation.lazy.grid.LazyGridState, index: Int) {
    gridState.scrollToItem(index)
    androidx.compose.runtime.withFrameNanos { }
    val info = gridState.layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val viewport = info.viewportEndOffset - info.viewportStartOffset
    gridState.scrollBy((item.offset.y - (viewport - item.size.height) / 2).toFloat())
}

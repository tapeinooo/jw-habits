package app.jwhabits.ui.bible

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.jwhabits.domain.BibleBook
import app.jwhabits.domain.BibleBooks
import app.jwhabits.domain.ChapterRef
import app.jwhabits.domain.JwLibrary
import app.jwhabits.domain.ReadingProgress
import app.jwhabits.ui.LocalUndoer
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun BibleScreen(vm: BibleViewModel = viewModel(factory = BibleViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    var bookNumber by rememberSaveable { mutableStateOf<Int?>(null) }

    val current = bookNumber
    if (current == null) {
        BookList(state, vm, onOpenBook = { bookNumber = it.number })
    } else {
        BackHandler { bookNumber = null }
        ChapterGrid(BibleBooks.book(current), state, vm, onBack = { bookNumber = null }, onOpenBook = { bookNumber = it.number })
    }
}

/** 「次：創世記 12章 ✓」。✓で記録、名前タップでその書へ、名前長押しで JW Library。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NextChapterRow(next: ChapterRef?, vm: BibleViewModel, onOpenBook: ((BibleBook) -> Unit)?, modifier: Modifier = Modifier) {
    if (next == null) return
    val undoer = LocalUndoer.current
    val context = LocalContext.current
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "次：${next.label}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).combinedClickable(
                    onClick = { onOpenBook?.invoke(next.book) },
                    onLongClick = { JwLibrary.open(context, JwLibrary.chapterUrl(next)) },
                ),
            )
            FilledTonalIconButton(onClick = { vm.toggle(next.index, undoer::offer) }) {
                Icon(Icons.Filled.Check, contentDescription = "${next.label} を記録")
            }
        }
    }
}

@Composable
private fun BookList(state: BibleUiState, vm: BibleViewModel, onOpenBook: (BibleBook) -> Unit) {
    var confirmNextRound by remember { mutableStateOf(false) }
    val full: (androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> GridItemSpan) = { GridItemSpan(maxLineSpan) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(76.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(span = full) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${state.round}周目 ・ ${state.readCount} / ${BibleBooks.totalChapters}章" +
                        if (state.todayCount > 0) " ・ 今日 ${state.todayCount}章" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.allDone) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("全${BibleBooks.totalChapters}章を読み終えました", style = MaterialTheme.typography.titleMedium)
                            Button(onClick = { confirmNextRound = true }) { Text("${state.round + 1}周目を始める") }
                        }
                    }
                } else {
                    NextChapterRow(state.next, vm, onOpenBook)
                }
            }
        }
        item(span = full) { SectionHeader("ヘブライ語聖書") }
        items(BibleBooks.all.filter { it.isHebrew }, key = { it.number }) { BookTile(it, state.readIn(it), onOpenBook) }
        item(span = full) { SectionHeader("ギリシャ語聖書") }
        items(BibleBooks.all.filterNot { it.isHebrew }, key = { it.number }) { BookTile(it, state.readIn(it), onOpenBook) }
    }

    if (confirmNextRound) {
        AlertDialog(
            onDismissRequest = { confirmNextRound = false },
            title = { Text("${state.round + 1}周目を始めますか") },
            text = { Text("${state.round}周目の記録は履歴に残ります。") },
            confirmButton = { TextButton(onClick = { confirmNextRound = false; vm.startNextRound() }) { Text("始める") } },
            dismissButton = { TextButton(onClick = { confirmNextRound = false }) { Text("やめる") } },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
}

/** 書のタイル。進み具合を下からの薄い塗りで示す。 */
@Composable
private fun BookTile(book: BibleBook, readCount: Int, onOpenBook: (BibleBook) -> Unit) {
    val done = readCount >= book.chapters
    val fraction = readCount.toFloat() / book.chapters
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = { onOpenBook(book) },
        shape = RoundedCornerShape(8.dp),
        color = if (done) scheme.primaryContainer else scheme.surfaceVariant,
        modifier = Modifier.height(60.dp),
    ) {
        Box {
            if (!done && fraction > 0f) {
                Box(
                    Modifier.fillMaxWidth().fillMaxHeight(fraction).align(Alignment.BottomCenter)
                        .background(scheme.primary.copy(alpha = 0.22f))
                )
            }
            Column(Modifier.fillMaxSize().padding(4.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    book.name, fontSize = 12.sp, lineHeight = 14.sp, maxLines = 2, textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    color = if (done) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                )
                if (readCount > 0 && !done) {
                    Text("$readCount/${book.chapters}", fontSize = 10.sp, color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

private enum class GridMode { NORMAL, RANGE, DATE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterGrid(
    book: BibleBook,
    state: BibleUiState,
    vm: BibleViewModel,
    onBack: () -> Unit,
    onOpenBook: (BibleBook) -> Unit,
) {
    var mode by rememberSaveable(book.number) { mutableStateOf(GridMode.NORMAL) }
    var menu by remember { mutableStateOf(false) }
    var dateTarget by remember { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            windowInsets = WindowInsets(0), // 外側の Scaffold がすでに余白を取っている
            title = { Text("${book.name}  ${state.readIn(book)}/${book.chapters}") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "書の一覧へ") } },
            actions = {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "メニュー") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("なぞってまとめて記録") }, onClick = { menu = false; mode = GridMode.RANGE })
                    DropdownMenuItem(text = { Text("読んだ日を直す") }, onClick = { menu = false; mode = GridMode.DATE })
                }
            },
        )
        when (mode) {
            GridMode.NORMAL -> NextChapterRow(state.next, vm, onOpenBook, Modifier.padding(horizontal = 12.dp))
            GridMode.RANGE -> ModeBanner("記録したい章を指でなぞってください") { mode = GridMode.NORMAL }
            GridMode.DATE -> ModeBanner("日付を直す章をタップしてください") { mode = GridMode.NORMAL }
        }
        Spacer(Modifier.height(8.dp))
        ChapterCells(
            book = book,
            state = state,
            mode = mode,
            vm = vm,
            onPickDate = { dateTarget = it },
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
    }

    dateTarget?.let { index ->
        val initial = state.read[index]?.date ?: vm.today()
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = initial.toEpochDay() * 86_400_000L,
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= vm.today().toEpochDay() * 86_400_000L
            },
        )
        DatePickerDialog(
            onDismissRequest = { dateTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let {
                        vm.setDate(index, Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    dateTarget = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { dateTarget = null }) { Text("やめる") } },
        ) {
            DatePicker(picker, title = { Text(BibleBooks.chapterAt(index).label, Modifier.padding(start = 24.dp, top = 16.dp)) })
        }
    }
}

@Composable
private fun ModeBanner(text: String, onDone: () -> Unit) {
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

/**
 * 章番号のマス目。
 * 通常：タップで記録／取り消し、長押しで JW Library。
 * なぞる：指を置いた章から今の章までを範囲として塗り、離すと記録する（端に来たら自動でスクロール）。
 * 日付：タップで日付を選ぶ。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChapterCells(
    book: BibleBook,
    state: BibleUiState,
    mode: GridMode,
    vm: BibleViewModel,
    onPickDate: (Int) -> Unit,
    modifier: Modifier,
) {
    val undoer = LocalUndoer.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val gridState = rememberLazyGridState()
    val today = vm.today()

    // なぞっている範囲（章番号 1始まり）
    var dragStart by remember { mutableStateOf<Int?>(null) }
    var dragEnd by remember { mutableStateOf<Int?>(null) }
    var pointer by remember { mutableStateOf(Offset.Zero) }
    var autoScroll by remember { mutableFloatStateOf(0f) }
    val edge = with(LocalDensity.current) { 56.dp.toPx() }
    val painted = dragStart?.let { s -> dragEnd?.let { e -> ReadingProgress.range(s, e) } }

    LaunchedEffect(autoScroll) {
        while (autoScroll != 0f) {
            gridState.scrollBy(autoScroll)
            chapterAt(gridState, pointer)?.let { dragEnd = it }
            delay(16)
        }
    }

    val dragModifier = if (mode == GridMode.RANGE) {
        Modifier.pointerInput(book.number) {
            detectDragGestures(
                onDragStart = { p ->
                    pointer = p
                    dragStart = chapterAt(gridState, p)
                    dragEnd = dragStart
                    if (dragStart != null) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDrag = { change, _ ->
                    pointer = change.position
                    chapterAt(gridState, pointer)?.let { dragEnd = it }
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
                        vm.markRange(ReadingProgress.range(s, e).map { book.indexOf(it) }, undoer::offer)
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
        items((1..book.chapters).toList(), key = { it }) { chapter ->
            val index = book.indexOf(chapter)
            val record = state.read[index]
            val isPainted = painted != null && chapter in painted
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
                        JwLibrary.open(context, JwLibrary.chapterUrl(book.number, chapter))
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
                    Text("$chapter", color = fg, fontWeight = if (record != null) FontWeight.Bold else FontWeight.Normal)
                    if (record != null && !isPainted) {
                        Text(shortDate(record.date, today), color = fg.copy(alpha = 0.8f), fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

/** 指の位置にある章（1始まり）。 */
private fun chapterAt(state: LazyGridState, p: Offset): Int? =
    state.layoutInfo.visibleItemsInfo.firstOrNull { item ->
        p.x >= item.offset.x && p.x < item.offset.x + item.size.width &&
            p.y >= item.offset.y && p.y < item.offset.y + item.size.height
    }?.key as? Int

private fun shortDate(d: LocalDate, today: LocalDate): String =
    if (d.year == today.year) "${d.monthValue}/${d.dayOfMonth}" else "${d.year % 100}/${d.monthValue}/${d.dayOfMonth}"

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
import androidx.compose.runtime.withFrameNanos
import app.jwhabits.domain.BibleFocus
import app.jwhabits.domain.ReadMark
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
import androidx.compose.foundation.layout.ColumnScope
import app.jwhabits.data.AppDatabase
import app.jwhabits.ui.units.GridMode
import app.jwhabits.ui.units.ModeBanner
import app.jwhabits.ui.units.ModeMenuItems
import app.jwhabits.ui.units.NextRoundDialog
import app.jwhabits.ui.units.NextRowFor
import app.jwhabits.ui.units.UnitCells
import app.jwhabits.ui.units.UnitDatePickerDialog
import app.jwhabits.ui.units.UnitTopBar
import app.jwhabits.ui.units.UnitUiState
import app.jwhabits.ui.units.UnitViewModel
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

/** 聖書通読のタブ。 */
@Composable
fun BibleScreen(
    vm: UnitViewModel = viewModel(
        key = UnitViewModel.key(AppDatabase.MATERIAL_BIBLE_READING),
        factory = UnitViewModel.factory(AppDatabase.MATERIAL_BIBLE_READING),
    ),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    BibleUnits(state, vm, listTitle = null, onExit = null)
}

/**
 * 66書のタイル → 章のマス目。聖書通読と、聖書全体の研究用の資料で共通。
 * onExit があれば（研究用の資料）、書の一覧にも上のバーを出して、戻る先を onExit にする。
 */
@Composable
fun BibleUnits(
    state: UnitUiState,
    vm: UnitViewModel,
    listTitle: String?,
    onExit: (() -> Unit)?,
    menuExtras: @Composable ColumnScope.(close: () -> Unit) -> Unit = {},
) {
    var bookNumber by rememberSaveable { mutableStateOf<Int?>(null) }
    // 開いたときに1度だけ：読みかけの書があればそれを開き、ちょうど読み終えた書なら一覧でその書を中央に
    var focused by rememberSaveable { mutableStateOf(false) }
    var centerBook by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(state.loaded) {
        if (!state.loaded || focused) return@LaunchedEffect
        focused = true
        BibleFocus.of(state.read.values.map { ReadMark(it.id, it.unitIndex) })?.let { f ->
            if (f.finished) centerBook = f.book.number else bookNumber = f.book.number
        }
    }
    val current = bookNumber
    if (current == null) {
        if (onExit != null) {
            BackHandler(onBack = onExit)
            Column(Modifier.fillMaxSize()) {
                UnitTopBar(listTitle.orEmpty(), "資料の一覧へ", onExit) { close -> menuExtras(close) }
                BookList(state, vm, onOpenBook = { bookNumber = it.number }, modifier = Modifier.weight(1f), centerBook = centerBook)
            }
        } else {
            BookList(state, vm, onOpenBook = { bookNumber = it.number }, centerBook = centerBook)
        }
    } else {
        BackHandler { bookNumber = null }
        ChapterGrid(BibleBooks.book(current), state, vm, onBack = { bookNumber = null }, onOpenBook = { bookNumber = it.number }, menuExtras)
    }
}

@Composable
private fun BookList(
    state: UnitUiState,
    vm: UnitViewModel,
    onOpenBook: (BibleBook) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
    centerBook: Int? = null,
) {
    var confirmNextRound by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    LaunchedEffect(centerBook) {
        val n = centerBook ?: return@LaunchedEffect
        // 並び：0 上の欄、1 ヘブライ語聖書の見出し、2..40 創世記〜マラキ、41 ギリシャ語聖書の見出し、42.. マタイ〜
        val index = if (n <= 39) 1 + n else 2 + n
        gridState.scrollToItem(index)
        withFrameNanos { }
        val info = gridState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return@LaunchedEffect
        val viewport = info.viewportEndOffset - info.viewportStartOffset
        gridState.scrollBy((item.offset.y - (viewport - item.size.height) / 2).toFloat())
    }
    val full: (androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> GridItemSpan) = { GridItemSpan(maxLineSpan) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(76.dp),
        state = gridState,
        modifier = modifier,
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
                    NextRowFor(state, vm, onClickLabel = { onOpenBook(BibleBooks.chapterAt(it).book) })
                }
            }
        }
        item(span = full) { SectionHeader("ヘブライ語聖書") }
        items(BibleBooks.all.filter { it.isHebrew }, key = { it.number }) { BookTile(it, state.readIn(it), onOpenBook) }
        item(span = full) { SectionHeader("ギリシャ語聖書") }
        items(BibleBooks.all.filterNot { it.isHebrew }, key = { it.number }) { BookTile(it, state.readIn(it), onOpenBook) }
    }

    if (confirmNextRound) NextRoundDialog(state.round, { confirmNextRound = false }) { vm.startNextRound() }
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

@Composable
private fun ChapterGrid(
    book: BibleBook,
    state: UnitUiState,
    vm: UnitViewModel,
    onBack: () -> Unit,
    onOpenBook: (BibleBook) -> Unit,
    menuExtras: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    var mode by rememberSaveable(book.number) { mutableStateOf(GridMode.NORMAL) }
    var dateTarget by remember { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxSize()) {
        UnitTopBar("${book.name}  ${state.readIn(book)}/${book.chapters}", "書の一覧へ", onBack) { close ->
            ModeMenuItems({ mode = it }, close)
            menuExtras(close)
        }
        if (mode == GridMode.NORMAL) {
            NextRowFor(state, vm, Modifier.padding(horizontal = 12.dp), onClickLabel = { onOpenBook(BibleBooks.chapterAt(it).book) })
        } else {
            ModeBanner(mode, "章") { mode = GridMode.NORMAL }
        }
        Spacer(Modifier.height(8.dp))
        UnitCells(
            count = book.chapters,
            state = state,
            mode = mode,
            vm = vm,
            indexOf = book::indexOf,
            onPickDate = { dateTarget = it },
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
    }

    dateTarget?.let { index ->
        UnitDatePickerDialog(
            title = BibleBooks.chapterAt(index).label,
            initial = state.read[index]?.date ?: vm.today(),
            today = vm.today(),
            onSave = { vm.setDate(index, it) },
            onDismiss = { dateTarget = null },
        )
    }
}

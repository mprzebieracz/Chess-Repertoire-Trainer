package com.example.chessrepertoiretrainer.core.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessrepertoiretrainer.core.chess.controller.ChessBoardController
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons

/**
 * Primary shared layout for all chess screens.
 *
 * Structure (top → bottom):
 * 1. [topBar] — title row with back button and action icons
 * 2. [engineSection] — horizontal eval bar + engine line, hidden when engine is off
 * 3. Chessboard — always full width, square
 * 4. [contentBar] — fills remaining space (PGN, status, comments, move list, …)
 * 5. [bottomBar] — fixed-height action buttons
 */
@Composable
fun ChessScreenLayout(
    chessCtrl: ChessBoardController,
    topBar: @Composable () -> Unit,
    engineSection: @Composable () -> Unit = {},
    contentBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        topBar()
        engineSection()
        Box(modifier = Modifier.fillMaxWidth()) {
            ChessboardUI(state = chessCtrl)
        }
        Box(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)) {
            contentBar()
        }
        bottomBar()
    }
}

// ---------------------------------------------------------------------------
// Shared top-bar helper
// ---------------------------------------------------------------------------

@Composable
fun ChessTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChessUiConstants.ChessTopBar.height)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    AppIcons.Back,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            actions()
        }
    }
}

// ---------------------------------------------------------------------------
// Shared bottom-bar helper
// ---------------------------------------------------------------------------

/**
 * Bottom action bar. Fill it with [BottomBarButton]s — each takes equal width via weight(1f).
 */
@Composable
fun ChessBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChessUiConstants.ChessBottomBar.height)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

/**
 * A single icon-above-label button that fills an equal share of the bottom bar.
 * Place it directly inside a [ChessBottomBar] lambda.
 */
@Composable
fun RowScope.BottomBarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val contentColor = if (enabled) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(28.dp),
            tint = contentColor,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = contentColor,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------------
// PGN viewer
// ---------------------------------------------------------------------------

@Composable
fun PgnViewer(
    sanHistory: List<String>,
    currentMoveIndex: Int,
    onMoveClick: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentMoveIndex) {
        if (currentMoveIndex >= 0) listState.animateScrollToItem(currentMoveIndex)
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = ChessUiConstants.ScreenChrome.Pgn.horizontalPadding,
                vertical = ChessUiConstants.ScreenChrome.Pgn.verticalPadding,
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(ChessUiConstants.ScreenChrome.Pgn.cornerRadius),
    ) {
        if (sanHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                Text(
                    text = "Waiting for moves...",
                    fontFamily = FontFamily.Monospace,
                    fontSize = ChessUiConstants.ScreenChrome.Pgn.textFontSize,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = ChessUiConstants.ScreenChrome.Pgn.textHorizontalPadding),
                )
            }
        }
        else {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(sanHistory) { index, san ->
                    val isCurrent = index == currentMoveIndex
                    val label = if (index % 2 == 0) "${index / 2 + 1}. $san" else san
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface,
                            )
                            .then(
                                if (onMoveClick != null) Modifier.clickable { onMoveClick(index) }
                                else Modifier.alpha(0.5f),
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = label,
                            fontFamily = FontFamily.Monospace,
                            fontSize = ChessUiConstants.ScreenChrome.Pgn.textFontSize,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Flowing PGN text view — left-to-right, top-to-bottom, vertically scrollable.
// Placeholder until a proper tree-aware PGN is implemented.
// ---------------------------------------------------------------------------

@Composable
fun PgnTextViewer(
    sanHistory: List<String>,
    currentMoveIndex: Int,
    onMoveClick: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(currentMoveIndex) {
        if (currentMoveIndex >= 0) scrollState.animateScrollTo(scrollState.maxValue)
    }

    val highlightBg = MaterialTheme.colorScheme.primaryContainer
    val highlightFg = MaterialTheme.colorScheme.onPrimaryContainer
    val normalFg = MaterialTheme.colorScheme.onBackground
    val dimFg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)

    val annotated = buildAnnotatedString {
        if (sanHistory.isEmpty()) {
            withStyle(SpanStyle(color = dimFg)) { append("No moves yet.") }
        }
        else {
            sanHistory.forEachIndexed { index, san ->
                if (index > 0) append(" ")
                if (index % 2 == 0) {
                    withStyle(SpanStyle(color = dimFg)) { append("${index / 2 + 1}.") }
                    append(" ")
                }
                val isCurrent = index == currentMoveIndex
                val spanStyle =
                    if (isCurrent) SpanStyle(background = highlightBg, color = highlightFg)
                    else SpanStyle(color = normalFg)
                if (onMoveClick != null) {
                    val idx = index
                    pushLink(LinkAnnotation.Clickable(
                        tag = idx.toString(),
                        styles = TextLinkStyles(spanStyle),
                        linkInteractionListener = { onMoveClick(idx) },
                    ))
                    append(san)
                    pop()
                }
                else {
                    withStyle(spanStyle) { append(san) }
                }
            }
        }
    }

    Text(
        text = annotated,
        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

// ---------------------------------------------------------------------------
// Legacy helpers — kept so callers that reference them still compile
// ---------------------------------------------------------------------------

@Composable
fun ScreenHeader(title: String, endContent: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = ChessUiConstants.ScreenChrome.Header.horizontalPadding,
                vertical = ChessUiConstants.ScreenChrome.Header.verticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        endContent()
    }
}

@Composable
fun BoardNavigationControls(onBack: () -> Unit, onForward: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ChessUiConstants.ScreenChrome.Navigation.rowPaddingVertical),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(ChessUiConstants.ScreenChrome.Navigation.buttonSize),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Navigate Back",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(ChessUiConstants.ScreenChrome.Navigation.iconSize),
            )
        }
        Spacer(modifier = Modifier.width(ChessUiConstants.ScreenChrome.Navigation.spacerWidth))
        IconButton(
            onClick = onForward,
            modifier = Modifier.size(ChessUiConstants.ScreenChrome.Navigation.buttonSize),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Navigate Forward",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(ChessUiConstants.ScreenChrome.Navigation.iconSize),
            )
        }
    }
}

@Composable
fun BoardActionButtons(
    onReset: () -> Unit,
    onFlip: () -> Unit,
    extraButtons: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ChessUiConstants.ScreenChrome.Actions.rowPadding),
        horizontalArrangement = Arrangement.spacedBy(ChessUiConstants.ScreenChrome.Actions.buttonSpacing),
    ) {
        Button(
            onClick = onReset,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
        ) { Text("Reset") }
        Button(onClick = onFlip, modifier = Modifier.weight(1f)) { Text("Flip") }
        extraButtons()
    }
}

@Composable
fun BoardBottomBar(
    chessCtrl: ChessBoardController,
    extraButtons: @Composable RowScope.() -> Unit = {},
) {
    Column {
        BoardNavigationControls(
            onBack = { chessCtrl.navigateBack() },
            onForward = { chessCtrl.navigateForward() },
        )
        BoardActionButtons(
            onReset = { chessCtrl.resetBoard() },
            onFlip = { chessCtrl.flipBoard() },
            extraButtons = extraButtons,
        )
    }
}
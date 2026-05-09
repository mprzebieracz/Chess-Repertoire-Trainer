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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.core.chess.controller.ChessBoardController

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
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            ChessboardUI(state = chessCtrl)
        }
        Box(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()) {
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}

// ---------------------------------------------------------------------------
// Shared bottom-bar helper
// ---------------------------------------------------------------------------

@Composable
fun ChessBottomBar(
    modifier: Modifier = Modifier,
    startContent: @Composable RowScope.() -> Unit = {},
    endContent: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) { startContent() }
        Row(verticalAlignment = Alignment.CenterVertically) { endContent() }
    }
}

// ---------------------------------------------------------------------------
// Navigation controls (compact, usable inside a Row)
// ---------------------------------------------------------------------------

@Composable
fun MoveNavControls(
    onBack: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(ChessUiConstants.ScreenChrome.Navigation.buttonSize),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous move",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(ChessUiConstants.ScreenChrome.Navigation.iconSize),
            )
        }
        IconButton(
            onClick = onForward,
            modifier = Modifier.size(ChessUiConstants.ScreenChrome.Navigation.buttonSize),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next move",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(ChessUiConstants.ScreenChrome.Navigation.iconSize),
            )
        }
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
        } else {
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

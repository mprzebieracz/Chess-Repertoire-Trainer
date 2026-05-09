package com.example.chessrepertoiretrainer.core.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.core.chess.controller.ChessBoardController

@Composable
fun ChessScreenLayout(
    title: String,
    chessCtrl: ChessBoardController,
    showPgnBar: Boolean = true,
    allowPgnNavigation: Boolean = true,
    evaluationBarFraction: Float? = null,
    titleEndContent: @Composable RowScope.() -> Unit = {},
    topContent: @Composable () -> Unit = {},
    midContent: @Composable () -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    extraButtons: @Composable RowScope.() -> Unit = {},
    showNavigationControls: Boolean = true,
    showBoardActionButtons: Boolean = true,
    enableScreenScroll: Boolean = true
) {
    val layoutModifier = if (enableScreenScroll) {
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    }
    else {
        Modifier.fillMaxSize()
    }

    Column(
        modifier = layoutModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        ScreenHeader(title, titleEndContent)

        topContent()

        if (showPgnBar) {
            PgnViewer(
                sanHistory = chessCtrl.sanHistory,
                currentMoveIndex = chessCtrl.currentMoveIndex,
                onMoveClick = if (allowPgnNavigation) ({ chessCtrl.navigateToMoveIndex(it) }) else null
            )
        }

        midContent()

        val boardPadding = Modifier.padding(
            horizontal = ChessUiConstants.ScreenChrome.BoardContainer.horizontalPadding,
            vertical = ChessUiConstants.ScreenChrome.BoardContainer.verticalPadding
        )
        val barWidth = ChessUiConstants.ScreenChrome.EvalBar.width
        val barSpacing = ChessUiConstants.ScreenChrome.EvalBar.spacing

        if (evaluationBarFraction != null) {
            // BoxWithConstraints lets us compute the board's square size (= available width minus bar)
            // so we can give the eval bar an explicit matching height.
            BoxWithConstraints(modifier = boardPadding.fillMaxWidth()) {
                val boardSize = maxWidth - barWidth - barSpacing
                val displayFraction = if (chessCtrl.isFlipped) 1f - evaluationBarFraction else evaluationBarFraction
                Row(
                    modifier = Modifier.fillMaxWidth().height(boardSize),
                    verticalAlignment = Alignment.Top
                ) {
                    EvaluationBar(
                        fraction = displayFraction,
                        modifier = Modifier.width(barWidth).fillMaxHeight()
                    )
                    Spacer(Modifier.width(barSpacing))
                    Box(modifier = Modifier.weight(1f)) {
                        ChessboardUI(state = chessCtrl)
                    }
                }
            }
        } else {
            Box(modifier = boardPadding) {
                ChessboardUI(state = chessCtrl)
            }
        }

        bottomContent()

        if (showNavigationControls) {
            BoardNavigationControls(
                onBack = { chessCtrl.navigateBack() },
                onForward = { chessCtrl.navigateForward() })
        }

        if (showBoardActionButtons) {
            BoardActionButtons(
                onReset = { chessCtrl.resetBoard() },
                onFlip = { chessCtrl.flipBoard() },
                extraButtons = extraButtons
            )
        }

        Spacer(modifier = Modifier.height(ChessUiConstants.ScreenChrome.Spacing.bottomSpacerHeight))
    }
}


@Composable
fun ScreenHeader(
    title: String,
    endContent: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = ChessUiConstants.ScreenChrome.Header.horizontalPadding,
                vertical = ChessUiConstants.ScreenChrome.Header.verticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        endContent()
    }
}

@Composable
fun PgnViewer(
    sanHistory: List<String>,
    currentMoveIndex: Int,
    onMoveClick: ((Int) -> Unit)?
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentMoveIndex) {
        if (currentMoveIndex >= 0) listState.animateScrollToItem(currentMoveIndex)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = ChessUiConstants.ScreenChrome.Pgn.horizontalPadding,
                vertical = ChessUiConstants.ScreenChrome.Pgn.verticalPadding
            )
            .height(ChessUiConstants.ScreenChrome.Pgn.height),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(ChessUiConstants.ScreenChrome.Pgn.cornerRadius)
    ) {
        if (sanHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                Text(
                    text = "Waiting for moves...",
                    fontFamily = FontFamily.Monospace,
                    fontSize = ChessUiConstants.ScreenChrome.Pgn.textFontSize,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = ChessUiConstants.ScreenChrome.Pgn.textHorizontalPadding)
                )
            }
        } else {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(sanHistory) { index, san ->
                    val isCurrent = index == currentMoveIndex
                    val label = if (index % 2 == 0) "${index / 2 + 1}. $san" else san
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .then(
                                if (onMoveClick != null) Modifier.clickable { onMoveClick(index) }
                                else Modifier.alpha(0.5f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontFamily = FontFamily.Monospace,
                            fontSize = ChessUiConstants.ScreenChrome.Pgn.textFontSize,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BoardNavigationControls(onBack: () -> Unit, onForward: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ChessUiConstants.ScreenChrome.Navigation.rowPaddingVertical),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(ChessUiConstants.ScreenChrome.Navigation.buttonSize)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Navigate Back",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(ChessUiConstants.ScreenChrome.Navigation.iconSize)
            )
        }
        Spacer(modifier = Modifier.width(ChessUiConstants.ScreenChrome.Navigation.spacerWidth))
        IconButton(
            onClick = onForward,
            modifier = Modifier.size(ChessUiConstants.ScreenChrome.Navigation.buttonSize)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Navigate Forward",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(ChessUiConstants.ScreenChrome.Navigation.iconSize)
            )
        }
    }
}

@Composable
fun BoardActionButtons(
    onReset: () -> Unit, onFlip: () -> Unit, extraButtons: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ChessUiConstants.ScreenChrome.Actions.rowPadding),
        horizontalArrangement = Arrangement.spacedBy(ChessUiConstants.ScreenChrome.Actions.buttonSpacing)
    ) {
        Button(
            onClick = onReset,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) { Text("Reset") }

        Button(
            onClick = onFlip, modifier = Modifier.weight(1f)
        ) { Text("Flip") }

        extraButtons()
    }
}

@Composable
fun BoardBottomBar(
    chessCtrl: ChessBoardController, extraButtons: @Composable RowScope.() -> Unit = {}
) {
    Column {
        BoardNavigationControls(
            onBack = { chessCtrl.navigateBack() },
            onForward = { chessCtrl.navigateForward() })
        BoardActionButtons(
            onReset = { chessCtrl.resetBoard() },
            onFlip = { chessCtrl.flipBoard() },
            extraButtons = extraButtons
        )
    }
}

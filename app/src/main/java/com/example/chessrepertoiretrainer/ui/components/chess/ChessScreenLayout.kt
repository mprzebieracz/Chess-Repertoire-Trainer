package com.example.chessrepertoiretrainer.ui.components.chess

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// ==========================================
// 1. GŁÓWNY UKŁAD (Layout)
// ==========================================
@Composable
fun ChessScreenLayout(
    title: String,
    chessCtrl: ChessBoardController,
    showPgnBar: Boolean = true,
    topContent: @Composable () -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    extraButtons: @Composable RowScope.() -> Unit = {},
    showNavigationControls: Boolean = true,
    showBoardActionButtons: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        ScreenHeader(title)

        topContent()

        if (showPgnBar) {
            PgnViewer(pgnText = chessCtrl.pgnState)
        }

        Box(
            modifier = Modifier.padding(
                horizontal = ChessUiConstants.ScreenChrome.BoardContainer.horizontalPadding,
                vertical = ChessUiConstants.ScreenChrome.BoardContainer.verticalPadding
            )
        ) {
            ChessboardUI(state = chessCtrl)
        }

        bottomContent()

        if (showNavigationControls) {
            BoardNavigationControls(
                onBack = { chessCtrl.navigateBack() },
                onForward = { chessCtrl.navigateForward() }
            )
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

// ==========================================
// 2. KLOCKI (Małe komponenty)
// ==========================================

@Composable
fun ScreenHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = ChessUiConstants.ScreenChrome.Header.horizontalPadding,
                vertical = ChessUiConstants.ScreenChrome.Header.verticalPadding
            )
    )
}

@Composable
fun PgnViewer(pgnText: String) {
    val scrollState = rememberScrollState()
    LaunchedEffect(pgnText) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = ChessUiConstants.ScreenChrome.Pgn.horizontalPadding,
                vertical = ChessUiConstants.ScreenChrome.Pgn.verticalPadding
            )
            .height(ChessUiConstants.ScreenChrome.Pgn.height),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ), shape = RoundedCornerShape(ChessUiConstants.ScreenChrome.Pgn.cornerRadius)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .padding(
                    horizontal = ChessUiConstants.ScreenChrome.Pgn.textHorizontalPadding,
                    vertical = ChessUiConstants.ScreenChrome.Pgn.textVerticalPadding
                )
        ) {
            Text(
                text = pgnText.ifEmpty { "Waiting for moves..." },
                fontFamily = FontFamily.Monospace,
                fontSize = ChessUiConstants.ScreenChrome.Pgn.textFontSize,
                maxLines = 1,
                softWrap = false,
                color = if (pgnText.isEmpty()) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        IconButton(onClick = onBack, modifier = Modifier.size(ChessUiConstants.ScreenChrome.Navigation.buttonSize)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Navigate Back",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(ChessUiConstants.ScreenChrome.Navigation.iconSize)
            )
        }
        Spacer(modifier = Modifier.width(ChessUiConstants.ScreenChrome.Navigation.spacerWidth))
        IconButton(onClick = onForward, modifier = Modifier.size(ChessUiConstants.ScreenChrome.Navigation.buttonSize)) {
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
        BoardNavigationControls(onBack = { chessCtrl.navigateBack() }, onForward = { chessCtrl.navigateForward() })
        BoardActionButtons(
            onReset = { chessCtrl.resetBoard() }, onFlip = { chessCtrl.flipBoard() }, extraButtons = extraButtons
        )
    }
}

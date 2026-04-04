package com.example.chessrepertoiretrainer.ui.components.chess

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==========================================
// 1. GŁÓWNY UKŁAD (Layout)
// ==========================================
@Composable
fun ChessScreenLayout(
  title: String,
  chessCtrl: ChessBoardController,
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

        PgnViewer(pgnText = chessCtrl.pgnState)

            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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

            Spacer(modifier = Modifier.height(80.dp))
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
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun PgnViewer(pgnText: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .heightIn(min = 60.dp, max = 120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = pgnText.ifEmpty { "Waiting for moves..." },
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                lineHeight = 20.sp,
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
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Navigate Back",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.width(48.dp))
        IconButton(onClick = onForward, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Navigate Forward",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun BoardActionButtons(
    onReset: () -> Unit,
    onFlip: () -> Unit,
    extraButtons: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onReset,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) { Text("Reset") }

        Button(
            onClick = onFlip,
            modifier = Modifier.weight(1f)
        ) { Text("Flip") }

        extraButtons()
    }
}
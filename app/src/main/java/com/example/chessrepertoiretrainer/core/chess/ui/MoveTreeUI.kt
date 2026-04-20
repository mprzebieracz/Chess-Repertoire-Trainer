package com.example.chessrepertoiretrainer.core.chess.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessrepertoiretrainer.core.database.entity.LineMove

@Composable
fun MoveTreeView(
    nodes: List<LineMove>, selectedMoveId: Long?, onMoveClicked: (LineMove) -> Unit, indent: Int = 0
) {
    Column(modifier = Modifier.padding(start = (indent * 12).dp)) {
        nodes.forEach { move ->
            val isSelected = move.id == selectedMoveId

            Text(
                text = move.moveSan,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().clickable { onMoveClicked(move) }.padding(vertical = 4.dp, horizontal = 8.dp))
        }
    }
}

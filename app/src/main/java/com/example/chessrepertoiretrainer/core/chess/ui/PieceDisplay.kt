package com.example.chessrepertoiretrainer.core.chess.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.github.bhlangonijr.chesslib.Piece

@Composable
fun PieceDisplay(piece: Piece, modifier: Modifier = Modifier) {
    val drawableRes = pieceDrawableRes(piece)
    if (drawableRes != 0) {
        Image(
            painter = painterResource(id = drawableRes), contentDescription = piece.name, modifier = modifier
        )
    }
}



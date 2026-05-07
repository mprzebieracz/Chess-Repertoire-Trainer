package com.example.chessrepertoiretrainer.feature.puzzles.domain.model

import com.example.chessrepertoiretrainer.core.database.entity.Puzzle
import com.github.bhlangonijr.chesslib.Side

data class PuzzleSessionState(
    val puzzle: Puzzle, val sanMoves: List<String>, val mySide: Side
)


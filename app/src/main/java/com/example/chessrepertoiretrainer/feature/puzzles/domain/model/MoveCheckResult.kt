package com.example.chessrepertoiretrainer.feature.puzzles.domain.model

sealed interface MoveCheckResult {
    data object Correct : MoveCheckResult
    data object Incorrect : MoveCheckResult
}
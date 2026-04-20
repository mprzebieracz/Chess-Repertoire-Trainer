package com.example.chessrepertoiretrainer.feature.puzzles.domain.config

data class PuzzleTrainingConfig(
	val maxInvalidPuzzleAttempts: Int = 20,
	val minUnsolvedPuzzles: Int = 1
)


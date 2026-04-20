package com.example.chessrepertoiretrainer.feature.puzzles.domain.usecase

import com.example.chessrepertoiretrainer.feature.puzzles.PuzzleRepository
import com.example.chessrepertoiretrainer.feature.puzzles.domain.config.PuzzleTrainingConfig

class EnsureMinUnsolvedUseCase(
	private val repository: PuzzleRepository,
	private val config: PuzzleTrainingConfig
) {
	suspend operator fun invoke(): Int {
		return repository.ensureMinUnsolvedPuzzles(config.minUnsolvedPuzzles)
	}
}


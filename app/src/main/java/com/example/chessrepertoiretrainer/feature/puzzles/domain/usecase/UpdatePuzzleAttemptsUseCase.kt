package com.example.chessrepertoiretrainer.feature.puzzles.domain.usecase

import com.example.chessrepertoiretrainer.feature.puzzles.PuzzleRepository

class UpdatePuzzleAttemptsUseCase(private val repository: PuzzleRepository) {
    suspend operator fun invoke(id: String, attempts: Int) {
        repository.updateAttempts(id = id, attempts = attempts)
    }
}

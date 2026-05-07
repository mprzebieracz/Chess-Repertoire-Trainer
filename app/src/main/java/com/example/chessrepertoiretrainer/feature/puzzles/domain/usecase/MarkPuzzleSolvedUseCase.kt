package com.example.chessrepertoiretrainer.feature.puzzles.domain.usecase

import com.example.chessrepertoiretrainer.feature.puzzles.PuzzleRepository

class MarkPuzzleSolvedUseCase(private val repository: PuzzleRepository) {
    suspend operator fun invoke(id: String, attempts: Int) {
        repository.markSolved(id = id, attempts = attempts)
    }
}

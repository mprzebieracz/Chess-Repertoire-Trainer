package com.example.chessrepertoiretrainer.feature.puzzles.domain.usecase

import com.example.chessrepertoiretrainer.feature.puzzles.domain.model.MoveCheckResult
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.MoveTrainingEngine

class CheckPuzzleMoveUseCase {
    operator fun invoke(result: MoveTrainingEngine.MoveResult): MoveCheckResult {
        return when (result) {
            is MoveTrainingEngine.MoveResult.Correct -> MoveCheckResult.Correct
            is MoveTrainingEngine.MoveResult.Incorrect -> MoveCheckResult.Incorrect
        }
    }
}
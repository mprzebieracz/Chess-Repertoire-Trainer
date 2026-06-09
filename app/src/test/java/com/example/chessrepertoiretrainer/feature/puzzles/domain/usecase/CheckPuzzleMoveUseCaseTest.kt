package com.example.chessrepertoiretrainer.feature.puzzles.domain.usecase

import com.example.chessrepertoiretrainer.core.chess.training.MoveTrainingEngine
import com.example.chessrepertoiretrainer.feature.puzzles.domain.model.MoveCheckResult
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckPuzzleMoveUseCaseTest {

    private val useCase = CheckPuzzleMoveUseCase()

    @Test
    fun `Correct result maps to MoveCheckResult Correct`() {
        val result = MoveTrainingEngine.MoveResult.Correct(
            userSan = "e4",
            expectedSan = "e4",
            isComplete = false
        )
        assertEquals(MoveCheckResult.Correct, useCase(result))
    }

    @Test
    fun `Incorrect result maps to MoveCheckResult Incorrect`() {
        val result = MoveTrainingEngine.MoveResult.Incorrect(
            userSan = "d4",
            expectedSan = "e4",
            fenBefore = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        )
        assertEquals(MoveCheckResult.Incorrect, useCase(result))
    }

    @Test
    fun `Correct with isComplete true still maps to Correct`() {
        val result = MoveTrainingEngine.MoveResult.Correct(
            userSan = "Qh5",
            expectedSan = "Qh5",
            isComplete = true
        )
        assertEquals(MoveCheckResult.Correct, useCase(result))
    }
}
package com.example.chessrepertoiretrainer.feature.puzzles.domain.usecase

import com.example.chessrepertoiretrainer.feature.puzzles.PuzzleRepository
import com.example.chessrepertoiretrainer.feature.puzzles.data.convertUciSequenceToSan
import com.example.chessrepertoiretrainer.feature.puzzles.domain.config.PuzzleTrainingConfig
import com.example.chessrepertoiretrainer.feature.puzzles.domain.model.PuzzleSessionState
import com.github.bhlangonijr.chesslib.Board

class LoadNextPuzzleUseCase(
	private val repository: PuzzleRepository,
	private val config: PuzzleTrainingConfig
) {
	suspend operator fun invoke(): PuzzleSessionState? {
		var attempts = 0

		while (attempts < config.maxInvalidPuzzleAttempts) {
			attempts++

			val puzzle = repository.getRandomUnsolvedPuzzle() ?: return null
			val rawTokens = puzzle.moves.split(" ").filter { it.isNotBlank() }
			if (rawTokens.isEmpty()) {
				repository.updatePuzzleStats(id = puzzle.id, isSolved = true, attempts = puzzle.attempts)
				continue
			}

			val sanMoves = convertUciSequenceToSan(puzzle.fen, rawTokens)
			if (sanMoves.isEmpty()) {
				repository.updatePuzzleStats(id = puzzle.id, isSolved = true, attempts = puzzle.attempts)
				continue
			}

			val board = Board().apply { loadFromFen(puzzle.fen) }
			return PuzzleSessionState(
				puzzle = puzzle,
				sanMoves = sanMoves,
				mySide = board.sideToMove
			)
		}

		return null
	}
}


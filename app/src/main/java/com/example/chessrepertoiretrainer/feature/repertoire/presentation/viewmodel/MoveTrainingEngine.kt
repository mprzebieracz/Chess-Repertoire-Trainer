package com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel

import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.toSan
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import kotlinx.coroutines.delay


class MoveTrainingEngine(
    private val chessController: DefaultChessBoardController,
    private val normalizeSan: (String) -> String
) {

    data class Config(
        val mySide: Side, val sanMoves: List<String>
    )

    sealed class MoveResult {
        data class Correct(val userSan: String, val expectedSan: String) : MoveResult()
        data class Incorrect(val userSan: String, val expectedSan: String) : MoveResult()
    }

    private var config: Config? = null
    private var currentIndex: Int = 0
    private var isAutoPlaying: Boolean = false

    private var resultListener: ((MoveResult) -> Unit)? = null

    init {
        chessController.onMoveListener = listener@{ _, san, _ ->
            if (isAutoPlaying) return@listener
            val result = handleUserMoveInternal(san) ?: return@listener
            resultListener?.invoke(result)
        }
    }

    fun setMoveResultListener(listener: (MoveResult) -> Unit) {
        this.resultListener = listener
    }

    /** Start a new training sequence from the beginning. */
    fun reset(newConfig: Config) {
        config = newConfig
        currentIndex = 0
    }

    fun isSequenceComplete(): Boolean {
        val cfg = config ?: return true
        return currentIndex >= cfg.sanMoves.size
    }

    private fun handleUserMoveInternal(san: String): MoveResult? {
        val cfg = config ?: return null
        if (currentIndex !in cfg.sanMoves.indices) return null

        val expectedSan = cfg.sanMoves[currentIndex]
        val isCorrect = normalizeSan(san) == normalizeSan(expectedSan)

        return if (isCorrect) {
            currentIndex++
            MoveResult.Correct(userSan = san, expectedSan = expectedSan)
        }
        else {
            MoveResult.Incorrect(userSan = san, expectedSan = expectedSan)
        }
    }

    /**
     * Auto-play all opponent replies from the current index until it is our
     * turn again or the sequence is exhausted.
     */
    suspend fun advanceOpponentReplies() {
        val cfg = config ?: return
        if (cfg.sanMoves.isEmpty()) return

        val board = chessController.getBoard()

        while (currentIndex < cfg.sanMoves.size && board.sideToMove != cfg.mySide) {
            val targetSan = cfg.sanMoves[currentIndex]
            val legalMove = board.legalMoves().firstOrNull { move ->
                board.toSan(move) == targetSan
            } ?: break

            delay(500)
            isAutoPlaying = true
            chessController.onMove(legalMove)
            isAutoPlaying = false

            currentIndex++
        }
    }

    /**
     * Play the next user move in the solution (from the current index) and
     * automatically follow up with all opponent replies. Returns true if the
     * entire sequence is finished after this step.
     */
    suspend fun playSolutionStep(): Boolean {
        val cfg = config ?: return true
        if (cfg.sanMoves.isEmpty()) return true

        val board = chessController.getBoard()

        // If it's currently the opponent's turn, catch up by auto-playing any
        // remaining opponent replies first.
        if (board.sideToMove != cfg.mySide) {
            advanceOpponentReplies()
        }

        if (currentIndex >= cfg.sanMoves.size) {
            return true
        }

        if (board.sideToMove != cfg.mySide) {
            // Still not our turn; nothing sensible to do.
            return isSequenceComplete()
        }

        val targetSan = cfg.sanMoves[currentIndex]
        val userMove = board.legalMoves().firstOrNull { move ->
            board.toSan(move) == targetSan
        } ?: run {
            // If we cannot find the move, treat the sequence as finished to
            // avoid getting stuck.
            return true
        }

        delay(300)
        isAutoPlaying = true
        chessController.onMove(userMove)
        isAutoPlaying = false
        currentIndex++

        // After the user's move, auto-play opponent replies.
        advanceOpponentReplies()

        return isSequenceComplete()
    }

    /**
     * Compute a simple hint by identifying the from-square of the next
     * expected move for our side, based on the current board state.
     */
    fun computeHintSquare(): Square? {
        val cfg = config ?: return null
        if (cfg.sanMoves.isEmpty()) return null
        if (currentIndex !in cfg.sanMoves.indices) return null

        val board = chessController.getBoard()
        if (board.sideToMove != cfg.mySide) return null

        val targetSan = cfg.sanMoves[currentIndex]
        val move = board.legalMoves().firstOrNull { legal ->
            board.toSan(legal) == targetSan
        } ?: return null

        return move.from
    }
}
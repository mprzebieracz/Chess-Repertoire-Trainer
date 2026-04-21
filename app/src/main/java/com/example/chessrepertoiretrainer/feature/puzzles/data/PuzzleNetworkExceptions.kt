package com.example.chessrepertoiretrainer.feature.puzzles.data

open class PuzzleNetworkException(
    message: String, cause: Throwable? = null
) : Exception(message, cause)

class PuzzleRateLimitException(
    message: String, cause: Throwable? = null
) : PuzzleNetworkException(message, cause)


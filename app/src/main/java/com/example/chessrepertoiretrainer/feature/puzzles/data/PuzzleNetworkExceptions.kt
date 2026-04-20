package com.example.chessrepertoiretrainer.feature.puzzles.data

/**
 * Exceptions used to signal network-related problems when fetching puzzles
 * from remote services such as Lichess.
 */
open class PuzzleNetworkException(
    message: String, cause: Throwable? = null
) : Exception(message, cause)

/** Specific subtype for HTTP 429 / rate-limit situations. */
class PuzzleRateLimitException(
    message: String, cause: Throwable? = null
) : PuzzleNetworkException(message, cause)


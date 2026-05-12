package com.example.chessrepertoiretrainer.core.engine

data class EngineAnalysis(
    val centipawns: Int?,
    val mateIn: Int?,
    val depth: Int,
    val line: String
) {
    val evaluationBarFraction: Float
        get() {
            if (mateIn != null) return if (mateIn > 0) 1.0f else 0.0f
            val cp = centipawns ?: return 0.5f
            return (cp.coerceIn(-1000, 1000) + 1000f) / 2000f
        }

    val scoreLabel: String
        get() {
            if (mateIn != null) return if (mateIn > 0) "M$mateIn" else "-M${-mateIn}"
            val cp = centipawns ?: return "0.00"
            val pawns = cp / 100.0
            return if (pawns >= 0) "+%.1f".format(pawns) else "%.1f".format(pawns)
        }
}

enum class EngineSearchState { IDLE, SEARCHING, COMPLETE }
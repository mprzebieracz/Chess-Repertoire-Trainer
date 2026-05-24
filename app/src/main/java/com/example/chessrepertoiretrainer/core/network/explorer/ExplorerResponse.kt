package com.example.chessrepertoiretrainer.core.network.explorer

data class ExplorerMove(
    val uci: String,
    val san: String,
    val white: Long,
    val draws: Long,
    val black: Long
) {
    val total: Long get() = white + draws + black
}

data class MasterGameEntry(
    val id: String,
    val white: String,
    val black: String,
    val year: Int,
    val winner: String?
)

data class ExplorerResponse(
    val white: Long,
    val draws: Long,
    val black: Long,
    val moves: List<ExplorerMove>,
    val opening: ExplorerOpening?,
    val topGames: List<MasterGameEntry> = emptyList(),
    val requiresAuth: Boolean = false
) {
    val total: Long get() = white + draws + black
}

data class ExplorerOpening(val eco: String?, val name: String?)

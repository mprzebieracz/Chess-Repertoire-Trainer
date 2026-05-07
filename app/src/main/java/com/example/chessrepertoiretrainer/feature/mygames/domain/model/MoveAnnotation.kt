package com.example.chessrepertoiretrainer.feature.mygames.domain.model

enum class ComplianceStatus {
    IN_BOOK,            // player move, FEN is in repertoire
    DEVIATION,          // player move, first off-book
    OUT_OF_BOOK,        // any move after a deviation (until transposition back)
    OPPONENT_IN_BOOK,   // opponent move, FEN is in repertoire
    OPPONENT_DEVIATION, // opponent move, first off-book
}

data class MoveAnnotation(
    val moveIndex: Int,
    val status: ComplianceStatus,
    val playedSan: String,
    val chapterIdForNavigation: Int?,
    val lineIdForNavigation: Int?
)

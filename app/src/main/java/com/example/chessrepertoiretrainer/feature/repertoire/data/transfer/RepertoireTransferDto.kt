package com.example.chessrepertoiretrainer.feature.repertoire.data.transfer

import kotlinx.serialization.Serializable

@Serializable
data class TransferEnvelope(
    val version: Int = 1,
    val repertoires: List<RepertoireDto>,
)

@Serializable
data class RepertoireDto(
    val name: String,
    val color: String,
    val chapters: List<ChapterDto>,
)

@Serializable
data class ChapterDto(
    val name: String,
    val sortOrder: Int,
    val primaryEcoCode: String? = null,
    val lines: List<LineDto>,
)

@Serializable
data class LineDto(
    val name: String,
    val sortOrder: Int = 0,
    val lastEcoCode: String? = null,
    val moves: List<LineMoveDto>,
)

@Serializable
data class LineMoveDto(
    val moveIndex: Int,
    val moveSan: String,
    val fen: String,
    val comment: String? = null,
    val arrows: String? = null,
)

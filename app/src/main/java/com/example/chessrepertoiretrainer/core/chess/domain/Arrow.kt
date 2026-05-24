package com.example.chessrepertoiretrainer.core.chess.domain

import com.github.bhlangonijr.chesslib.Square

data class Arrow(val from: Square, val to: Square, val alpha: Float = 1f) {
    val isHighlight: Boolean get() = from == to
}

fun List<Arrow>.serializeArrows(): String =
    joinToString(",") { "${it.from.name.lowercase()}${it.to.name.lowercase()}" }

fun String?.parseArrows(): List<Arrow> =
    orEmpty().split(",").mapNotNull { token ->
        if (token.length != 4) return@mapNotNull null
        runCatching {
            Arrow(
                Square.valueOf(token.substring(0, 2).uppercase()),
                Square.valueOf(token.substring(2, 4).uppercase())
            )
        }.getOrNull()
    }

/** Returns the list with [arrow] removed if present, otherwise with it appended. */
fun List<Arrow>.toggle(arrow: Arrow): List<Arrow> =
    if (contains(arrow)) filterNot { it == arrow } else this + arrow

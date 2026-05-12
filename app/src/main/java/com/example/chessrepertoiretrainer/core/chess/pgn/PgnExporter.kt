package com.example.chessrepertoiretrainer.core.chess.pgn

import com.example.chessrepertoiretrainer.core.database.entity.LineMove

object PgnExporter {

    fun export(moves: List<LineMove>, headers: Map<String, String> = emptyMap()): String {
        val sb = StringBuilder()

        val effectiveHeaders = if (headers.isEmpty()) {
            mapOf(
                "Event" to "?", "Site" to "?", "Date" to "????.??.??",
                "Round" to "?", "White" to "?", "Black" to "?", "Result" to "*"
            )
        } else {
            headers
        }

        for ((key, value) in effectiveHeaders) {
            sb.append("[$key \"$value\"]\n")
        }
        sb.append("\n")

        moves.forEachIndexed { index, move ->
            if (index % 2 == 0) {
                sb.append("${index / 2 + 1}. ")
            }
            sb.append(move.moveSan)
            if (!move.comment.isNullOrBlank()) {
                sb.append(" {${move.comment}}")
            }
            sb.append(" ")
        }

        sb.append(effectiveHeaders["Result"] ?: "*")
        return sb.toString().trimEnd()
    }
}
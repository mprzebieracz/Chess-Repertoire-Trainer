package com.example.chessrepertoiretrainer.core.chess.pgn.parse

class PgnTextProcessor {

    fun preprocess(raw: String): String {
        var text = raw.replace("\r\n", "\n").replace('\r', '\n').replace(' ', ' ')
        val inlineEngineTagRegex = Regex("""\[%[^]]*]""")
        text = text.replace(inlineEngineTagRegex, "")
        return text.lines().joinToString("\n") { it.trimEnd() }
    }

    fun splitIntoGames(cleanedPgn: String): List<String> {
        val games = mutableListOf<StringBuilder>()
        var current: StringBuilder? = null

        for (rawLine in cleanedPgn.lines()) {
            val line = rawLine.trim()
            if (line.startsWith("[") && line.endsWith("]")) {
                if (current != null && current.isNotEmpty()) {
                    games.add(current)
                }
                current = StringBuilder()
            }

            if (current != null) {
                current.append(rawLine).append('\n')
            }
        }

        if (current != null && current.isNotEmpty()) {
            games.add(current)
        }

        return games.map { it.toString().trim() }
    }
}

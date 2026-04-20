package com.example.chessrepertoiretrainer.feature.repertoire.data.pgn

class PgnTextProcessor {

    /**
     * PGN preprocessor: strip GUI/engine inline tags like [%eval ...] while keeping
     * standard headers and curly-brace comments.
     */
    fun preprocess(raw: String): String {
        var text = raw.replace("\r\n", "\n").replace('\r', '\n').replace('\u00A0', ' ')
        val inlineEngineTagRegex = Regex("""\[%[^]]*]""")
        text = text.replace(inlineEngineTagRegex, "")
        return text.lines().joinToString("\n") { it.trimEnd() }
    }

    /**
     * Splits whole PGN text into games. Every game starts from a header line
     * enclosed in square brackets.
     */
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


package com.example.chessrepertoiretrainer.core.chess.pgn.extract

class PGNExtractor {
    companion object {
        fun extractSanMovesFromPgn(pgn: String): List<String> {
            if (pgn.isBlank()) return emptyList()

            var inBody = false
            val sanMoves = mutableListOf<String>()
            var inBraceComment = false

            fun isGameResultToken(token: String): Boolean =
                token == "1-0" || token == "0-1" || token == "1/2-1/2" || token == "*"

            fun isMoveNumberToken(token: String): Boolean {
                if (token.isEmpty()) return false
                var i = 0
                val n = token.length
                while (i < n && token[i].isDigit()) i++
                if (i == 0 || i >= n) return false
                if (token[i] != '.') return false
                while (i < n && token[i] == '.') i++
                return i == n
            }

            for (rawLine in pgn.lineSequence()) {
                val line = rawLine.trim()
                if (!inBody) {
                    if (line.startsWith("[") || line.isBlank()) {
                        // keep skipping header / blank lines until movetext starts
                    } else {
                        inBody = true
                    }
                }

                if (inBody) {
                    var sanitizedLine = rawLine
                    while (true) {
                        val start = sanitizedLine.indexOf("[%")
                        if (start == -1) break
                        val end = sanitizedLine.indexOf(']', start + 2)
                        if (end == -1) {
                            sanitizedLine = sanitizedLine.removeRange(start, sanitizedLine.length)
                            break
                        } else {
                            sanitizedLine = sanitizedLine.removeRange(start, end + 1)
                        }
                    }

                    var stopParsing = false
                    for (piece in sanitizedLine.splitToSequence(' ', '\t')) {
                        val token = piece.trim()
                        if (token.isNotEmpty()) {
                            if (inBraceComment) {
                                if (token.contains('}')) {
                                    inBraceComment = false
                                }
                            } else if (token.startsWith("{")) {
                                if (!token.contains('}')) {
                                    inBraceComment = true
                                }
                            } else if (token.startsWith('$') && token.drop(1)
                                    .all { it.isDigit() }
                            ) {
                                // skip numeric annotation glyphs
                            } else if (isGameResultToken(token)) {
                                stopParsing = true
                                break
                            } else {
                                val cleaned = token.trim('(', ')')
                                if (cleaned.isNotEmpty() && !isMoveNumberToken(cleaned)) {
                                    sanMoves += cleaned
                                }
                            }
                        }
                    }

                    if (stopParsing) break
                }
            }

            return sanMoves
        }
    }
}
package com.example.chessrepertoiretrainer.feature.repertoire.data.pgn

class PgnMovetextParser {

    data class ParsedMove(val san: String, val comment: String?)

    data class ParseResult(
        val lines: List<List<ParsedMove>>, val nextIndex: Int
    )

    fun parseLinesFromMovetext(text: String): List<List<ParsedMove>> {
        val (lines, _) = parseMovetextRecursive(text, 0, emptyList())
        return lines
    }

    fun parseMovetextRecursive(
        text: String, startIndex: Int, parentPrefix: List<ParsedMove>
    ): ParseResult {
        val variationLines = mutableListOf<List<ParsedMove>>()
        val current = parentPrefix.map { it.copy() }.toMutableList()
        var i = startIndex
        var pendingCommentForNext: String? = null

        fun appendCommentToLast(textComment: String) {
            if (current.isNotEmpty()) {
                val last = current.last()
                val combined = if (last.comment.isNullOrEmpty()) {
                    textComment
                }
                else {
                    last.comment + "\n" + textComment
                }
                current[current.lastIndex] = last.copy(comment = combined)
            }
            else {
                pendingCommentForNext = if (pendingCommentForNext == null) {
                    textComment
                }
                else {
                    pendingCommentForNext + "\n" + textComment
                }
            }
        }

        val length = text.length

        while (i < length) {
            val c = text[i]

            when {
                c.isWhitespace() -> i++
                c == '{' -> {
                    val start = i + 1
                    var j = start
                    while (j < length && text[j] != '}') {
                        j++
                    }
                    val commentText = text.substring(start, minOf(j, length)).trim()
                    if (commentText.isNotEmpty()) {
                        appendCommentToLast(commentText)
                    }
                    i = if (j < length) j + 1 else j
                }

                c == '[' -> {
                    var j = i + 1
                    while (j < length && text[j] != ']') {
                        j++
                    }
                    i = if (j < length) j + 1 else j
                }

                c == '(' -> {
                    val parentForVariation = if (current.isNotEmpty()) {
                        current.dropLast(1)
                    }
                    else {
                        current
                    }

                    val result = parseMovetextRecursive(text, i + 1, parentForVariation)
                    variationLines.addAll(result.lines)
                    i = result.nextIndex
                }

                c == ')' -> {
                    i++
                    break
                }

                text.startsWith("1-0", i) || text.startsWith("0-1", i) || text.startsWith(
                    "1/2-1/2",
                    i
                ) || c == '*' -> {
                    i += when {
                        text.startsWith("1-0", i) -> 3
                        text.startsWith("0-1", i) -> 3
                        text.startsWith("1/2-1/2", i) -> "1/2-1/2".length
                        else -> 1
                    }
                    break
                }

                c == '$' -> {
                    var j = i + 1
                    while (j < length && text[j].isDigit()) {
                        j++
                    }
                    i = j
                }

                c.isDigit() -> {
                    var j = i
                    while (j < length && text[j].isDigit()) {
                        j++
                    }
                    while (j < length && text[j] == '.') {
                        j++
                    }
                    i = j
                }

                else -> {
                    var j = i
                    while (j < length && !text[j].isWhitespace() && text[j] != '{' && text[j] != '}' && text[j] != '(' && text[j] != ')') {
                        j++
                    }
                    val san = text.substring(i, j).trim()
                    if (san.isNotEmpty()) {
                        val moveComment = pendingCommentForNext
                        pendingCommentForNext = null
                        current.add(ParsedMove(san, moveComment))
                    }
                    i = j
                }
            }
        }

        val allLines = mutableListOf<List<ParsedMove>>()
        if (current.isNotEmpty()) {
            allLines.add(current.toList())
        }
        allLines.addAll(variationLines)

        return ParseResult(allLines, i)
    }
}


package com.example.chessrepertoiretrainer.feature.repertoire.data

import android.util.Log
import com.example.chessrepertoiretrainer.core.chess.pgn.parse.PgnLineResolver
import com.example.chessrepertoiretrainer.core.chess.pgn.parse.PgnMovetextParser
import com.example.chessrepertoiretrainer.core.chess.pgn.parse.PgnTextProcessor
import com.example.chessrepertoiretrainer.core.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.feature.repertoire.data.pgn.PgnImportWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PgnImporter(private val repertoireDao: RepertoireDao) {

    private val textProcessor = PgnTextProcessor()
    private val parser = PgnMovetextParser()
    private val lineResolver = PgnLineResolver()
    private val importWriter = PgnImportWriter(repertoireDao)

    suspend fun importPgnToChapter(pgnString: String, chapterId: Int) =
        withContext(Dispatchers.IO) {
            if (pgnString.isBlank()) return@withContext

            try {
                val cleanedPgn = textProcessor.preprocess(pgnString)
                val games = textProcessor.splitIntoGames(cleanedPgn)
                if (games.isEmpty()) {
                    Log.e("PgnImporter", "No games found in PGN.")
                    return@withContext
                }

                val chapter = repertoireDao.getChapterById(chapterId)
                val chapterName = chapter?.name ?: "Line"
                var nextLineNumber = repertoireDao.getLineCountForChapter(chapterId) + 1

                games.forEach { gameText ->
                    if (gameText.isBlank()) return@forEach

                    val hasMovetext = gameText.lines().any { rawLine ->
                        val line = rawLine.trim()
                        line.isNotEmpty() && !(line.startsWith("[") && line.endsWith("]"))
                    }

                    if (!hasMovetext) {
                        return@forEach
                    }

                    val insertedForGame = parseSingleGameAndInsert(
                        gameText = gameText,
                        chapterId = chapterId,
                        chapterName = chapterName,
                        startingLineNumber = nextLineNumber
                    )
                    nextLineNumber += insertedForGame
                }

            }
            catch (e: Exception) {
                Log.e("PgnImporter", "PGN import error: ${e.message}", e)
            }
        }

    private suspend fun parseSingleGameAndInsert(
        gameText: String,
        chapterId: Int,
        chapterName: String,
        startingLineNumber: Int
    ): Int {
        val (headerMap, body) = extractHeadersAndBody(gameText)
        if (body.isBlank()) {
            return 0
        }

        val allLines = parser.parseLinesFromMovetext(body)
        if (allLines.isEmpty()) {
            Log.e("PgnImporter", "Could not extract moves from PGN.")
            return 0
        }

        var insertedLinesCount = 0
        var currentLineNumber = startingLineNumber

        allLines.forEach { parsedMoves ->
            if (parsedMoves.isEmpty()) return@forEach

            val resolvedMoves = lineResolver.resolveLine(parsedMoves, headerMap["FEN"])
            if (resolvedMoves.isEmpty()) {
                return@forEach
            }

            val lineName = "$chapterName #$currentLineNumber"

            importWriter.insertLineWithMoves(
                chapterId = chapterId,
                lineName = lineName,
                moves = resolvedMoves
            )

            insertedLinesCount++
            currentLineNumber++
        }

        return insertedLinesCount
    }

    internal fun extractHeadersAndBody(gameText: String): Pair<Map<String, String>, String> {
        val headerMap = mutableMapOf<String, String>()
        val bodyBuilder = StringBuilder()
        var inHeaderSection = true

        gameText.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) {
                if (inHeaderSection) {
                    inHeaderSection = false
                }
                return@forEach
            }

            if (inHeaderSection && line.startsWith("[") && line.endsWith("]")) {
                val match = TAG_REGEX.matchEntire(line)
                if (match != null) {
                    headerMap[match.groupValues[1]] = match.groupValues[2]
                }
            }
            else {
                inHeaderSection = false
                if (bodyBuilder.isNotEmpty()) {
                    bodyBuilder.append('\n')
                }
                bodyBuilder.append(line)
            }
        }

        return headerMap to bodyBuilder.toString()
    }

    private companion object {
        private val TAG_REGEX = Regex("""\[(\w+)\s+"(.*)"]""")
    }
}
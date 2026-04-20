package com.example.chessrepertoiretrainer.feature.repertoire.data

import android.util.Log
import com.example.chessrepertoiretrainer.core.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.feature.repertoire.data.pgn.PgnImportWriter
import com.example.chessrepertoiretrainer.feature.repertoire.data.pgn.PgnLineResolver
import com.example.chessrepertoiretrainer.feature.repertoire.data.pgn.PgnMovetextParser
import com.example.chessrepertoiretrainer.feature.repertoire.data.pgn.PgnTextProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PgnImporter(private val repertoireDao: RepertoireDao) {

    private val textProcessor = PgnTextProcessor()
    private val parser = PgnMovetextParser()
    private val lineResolver = PgnLineResolver()
    private val importWriter = PgnImportWriter(repertoireDao)

    suspend fun importPgnToChapter(pgnString: String, chapterId: Int) = withContext(Dispatchers.IO) {
        if (pgnString.isBlank()) return@withContext

        try {
            val cleanedPgn = textProcessor.preprocess(pgnString)
            val games = textProcessor.splitIntoGames(cleanedPgn)
            if (games.isEmpty()) {
                Log.e("PgnImporter", "Nie znaleziono zadnych partii w PGN.")
                return@withContext
            }

            val chapter = repertoireDao.getChapterById(chapterId)
            val chapterName = chapter?.name ?: "Line"
            var nextLineNumber = repertoireDao.getLineCountForChapter(chapterId) + 1

            var gamesWithMovetext = 0
            var gamesWithoutMovetext = 0
            var totalLinesInserted = 0

            games.forEach { gameText ->
                if (gameText.isBlank()) return@forEach

                val hasMovetext = gameText.lines().any { rawLine ->
                    val line = rawLine.trim()
                    line.isNotEmpty() && !(line.startsWith("[") && line.endsWith("]"))
                }

                if (!hasMovetext) {
                    gamesWithoutMovetext++
                    return@forEach
                }

                gamesWithMovetext++
                val insertedForGame = parseSingleGameAndInsert(
                    gameText = gameText,
                    chapterId = chapterId,
                    chapterName = chapterName,
                    startingLineNumber = nextLineNumber
                )
                totalLinesInserted += insertedForGame
                nextLineNumber += insertedForGame
            }

            Log.i(
                "PgnImporter",
                "Import PGN zakonczony. Partii z trescia=$gamesWithMovetext, bez tresci=$gamesWithoutMovetext, dodanych linii=$totalLinesInserted"
            )
        } catch (e: Exception) {
            Log.e("PgnImporter", "Blad importu PGN: ${e.message}", e)
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
            Log.w("PgnImporter", "Brak tresci partii w PGN (blok zostaje pominiety).")
            return 0
        }

        val allLines = parser.parseLinesFromMovetext(body)
        if (allLines.isEmpty()) {
            Log.e("PgnImporter", "Nie udalo sie wyodrebnic ruchow z PGN.")
            return 0
        }

        val white = headerMap["White"] ?: "White"
        val black = headerMap["Black"] ?: "Black"
        val event = headerMap["Event"] ?: "Imported"
        val baseTitle = "$white - $black ($event)"

        var insertedLinesCount = 0
        var currentLineNumber = startingLineNumber

        allLines.forEach { parsedMoves ->
            if (parsedMoves.isEmpty()) return@forEach

            val resolvedMoves = lineResolver.resolveLine(parsedMoves, headerMap["FEN"])
            if (resolvedMoves.isEmpty()) {
                return@forEach
            }

            val lineName = "$chapterName #$currentLineNumber"
            Log.d(
                "PgnImporter",
                "Rozwiazana linia (${resolvedMoves.size} ruchow) dla '$baseTitle' jako '$lineName': " +
                    resolvedMoves.joinToString(" ") { it.san }
            )

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

    private fun extractHeadersAndBody(gameText: String): Pair<Map<String, String>, String> {
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
            } else {
                inHeaderSection = false
                if (bodyBuilder.isNotEmpty()) {
                    bodyBuilder.append('\n')
                }
                bodyBuilder.append(line)
            }
        }

        return headerMap to bodyBuilder.toString()
    }

    // Kept for parser unit tests and compatibility.
    internal fun parseLinesFromMovetext(text: String): List<List<PgnMovetextParser.ParsedMove>> =
        parser.parseLinesFromMovetext(text)

    internal fun parseMovetextRecursive(
        text: String,
        startIndex: Int,
        parentPrefix: List<PgnMovetextParser.ParsedMove>
    ): PgnMovetextParser.ParseResult = parser.parseMovetextRecursive(text, startIndex, parentPrefix)

    private companion object {
        private val TAG_REGEX = Regex("""\[(\w+)\s+"(.*)"]""")
    }
}

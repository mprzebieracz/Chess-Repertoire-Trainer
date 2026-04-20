package com.example.chessrepertoiretrainer.data

import android.content.Context
import android.util.Log
import com.example.chessrepertoiretrainer.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.database.entities.Line
import com.example.chessrepertoiretrainer.database.entities.LineMove
import com.example.chessrepertoiretrainer.ui.components.chess.moveFromSan
import com.example.chessrepertoiretrainer.ui.components.chess.toSan
import com.github.bhlangonijr.chesslib.Board
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PgnImporter(private val repertoireDao: RepertoireDao) {

    suspend fun importPgnToChapter(context: Context, pgnString: String, chapterId: Int) = withContext(Dispatchers.IO) {
        if (pgnString.isBlank()) return@withContext

        try {
            val cleanedPgn = preprocessPgn(pgnString)

            // Podziel cały plik PGN na poszczególne partie (gry) i sparsuj każdą z osobna.
            val games = splitIntoGames(cleanedPgn)
            if (games.isEmpty()) {
                Log.e("PgnImporter", "Nie znaleziono żadnych partii w PGN.")
                return@withContext
            }

            // Ustal bazową nazwę rozdziału i aktualną liczbę linii w tym rozdziale,
            // aby nowe linie (również z PGN) były nazywane spójnie jak przy ręcznym dodawaniu:
            // "{chapter name} #{nr line}".
            val chapter = repertoireDao.getChapterById(chapterId)
            val chapterName = chapter?.name ?: "Line"
            var nextLineNumber = repertoireDao.getLineCountForChapter(chapterId) + 1

            var gamesWithMovetext = 0
            var gamesWithoutMovetext = 0
            var totalLinesInserted = 0

            games.forEach { gameText ->
                if (gameText.isBlank()) return@forEach

                // Sprawdź, czy dany blok ma jakąkolwiek treść poza nagłówkami.
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
                    cleanedPgn = gameText, chapterId = chapterId, chapterName = chapterName, startingLineNumber = nextLineNumber
                )
                totalLinesInserted += insertedForGame
                nextLineNumber += insertedForGame
            }

            Log.i(
                "PgnImporter",
                "Import PGN zakończony. Partii z treścią=$gamesWithMovetext, bez treści=$gamesWithoutMovetext, dodanych linii=$totalLinesInserted"
            )
        }
        catch (e: Exception) {
            Log.e("PgnImporter", "Błąd importu PGN: ${e.message}", e)
        }
    }

    /**
     * PGN preprocessor: strip GUI/engine inline tags like [%eval ...] while keeping
     * standard headers and curly-brace comments.
     */
    private fun preprocessPgn(raw: String): String {
        var text = raw.replace("\r\n", "\n").replace('\r', '\n').replace('\u00A0', ' ')

        val inlineEngineTagRegex = Regex("""\[%[^]]*]""")
        text = text.replace(inlineEngineTagRegex, "")

        text = text.lines().joinToString("\n") { it.trimEnd() }

        return text
    }

    /**
     * Dzieli cały tekst PGN na poszczególne partie. Przyjmujemy, że każda partia
     * zaczyna się od nagłówka [Event "..."] lub innego wiersza w nawiasach kwadratowych.
     */
    private fun splitIntoGames(cleanedPgn: String): List<String> {
        val games = mutableListOf<StringBuilder>()
        var current: StringBuilder? = null

        for (rawLine in cleanedPgn.lines()) {
            val line = rawLine.trim()
            if (line.startsWith("[") && line.endsWith("]")) {
                // Nowy blok nagłówków oznacza początek kolejnej partii.
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

    /**
     * Główny parser PGN dla pojedynczej partii.
     * Buduje wszystkie linie (główna + warianty) i zapisuje je w bazie.
     */
    private suspend fun parseSingleGameAndInsert(
        cleanedPgn: String, chapterId: Int, chapterName: String, startingLineNumber: Int
    ): Int {
        val headerMap = mutableMapOf<String, String>()
        val bodyBuilder = StringBuilder()
        var inHeaderSection = true

        for (rawLine in cleanedPgn.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) {
                if (inHeaderSection) {
                    // blank line separates headers from movetext
                    inHeaderSection = false
                }
                continue
            }

            if (inHeaderSection && line.startsWith("[") && line.endsWith("]")) {
                val match = TAG_REGEX.matchEntire(line)
                if (match != null) {
                    val key = match.groupValues[1]
                    val value = match.groupValues[2]
                    headerMap[key] = value
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

        val body = bodyBuilder.toString()
        if (body.isBlank()) {
            // To nie powinno się zdarzać często, bo puste partie odfiltrowujemy już
            // na poziomie importPgnToChapter. Traktujemy to więc jako łagodne ostrzeżenie.
            Log.w("PgnImporter", "Brak treści partii w PGN (blok zostaje pominięty).")
            return 0
        }

        // 1. Sparsuj całe drzewo partii (główna linia + wszystkie warianty) na listę linii
        val allLines = parseLinesFromMovetext(body)

        if (allLines.isEmpty()) {
            Log.e("PgnImporter", "Nie udało się wyodrębnić ruchów z PGN.")
            return 0
        }

        Log.d("PgnImporter", "Sparsowane linie z PGN (${allLines.size}):")
        allLines.forEachIndexed { index, parsedMoves ->
            val sanLine = parsedMoves.joinToString(separator = " ") { it.san }
            Log.d("PgnImporter", "Linia ${index + 1}: $sanLine")
        }

        val white = headerMap["White"] ?: "White"
        val black = headerMap["Black"] ?: "Black"
        val event = headerMap["Event"] ?: "Imported"
        val baseTitle = "$white - $black ($event)"

        var insertedLinesCount = 0
        var currentLineNumber = startingLineNumber

        // 2. Dla każdej linii odtwarzamy ruchy na szachownicy i zapisujemy jako osobną linię w rozdziale
        allLines.forEachIndexed { index, parsedMoves ->
            if (parsedMoves.isEmpty()) return@forEachIndexed

            // Nazwa linii w bazie ma być spójna z ręcznym dodawaniem linii:
            // "{chapter name} #{nr line}", niezależnie od tego, z której partii PGN pochodzi.
            val lineName = "$chapterName #$currentLineNumber"

            val board = Board()
            headerMap["FEN"]?.takeIf { it.isNotBlank() }?.let { fenTag ->
                try {
                    board.loadFromFen(fenTag)
                }
                catch (e: Exception) {
                    Log.e("PgnImporter", "Nie udało się załadować FEN z tagu FEN: $fenTag", e)
                }
            }

            val resolvedMoves = mutableListOf<ResolvedMove>()

            for (parsed in parsedMoves) {
                val matchingMove = board.moveFromSan(parsed.san)

                if (matchingMove == null) {
                    Log.e(
                        "PgnImporter", "Nie znaleziono dopasowania SAN dla ruchu '${parsed.san}' w pozycji ${board.fen}"
                    )
                    // Kończymy budowę tej linii w tym miejscu – zachowujemy już
                    // dopasowane ruchy zamiast całkowicie ją odrzucać.
                    break
                }

                val sanFromBoard = board.toSan(matchingMove)
                board.doMove(matchingMove)
                val fenAfter = board.fen

                resolvedMoves.add(ResolvedMove(sanFromBoard, fenAfter, parsed.comment))
            }

            if (resolvedMoves.isEmpty()) {
                return@forEachIndexed
            }

            // Odfiltruj bardzo krótkie linie, które zwykle powstają po wczesnym
            // błędzie dopasowania SAN w zawiłych wariantach. Dzięki temu
            // unikamy "pustych" lub mało przydatnych linii.
            if (resolvedMoves.size < MIN_MOVES_PER_LINE) {
                Log.d(
                    "PgnImporter", "Pomijam zbyt krótką linię: ${resolvedMoves.size} ruchów dla partii $baseTitle"
                )
                return@forEachIndexed
            }

            Log.d(
                "PgnImporter",
                "Rozwiązana linia (${resolvedMoves.size} ruchów) dla '$baseTitle' jako '$lineName': " + resolvedMoves.joinToString(separator = " ") { it.san })

            val lineId = repertoireDao.insertLine(
                Line(
                    chapterId = chapterId,
                    name = lineName,
                    nextReviewDate = System.currentTimeMillis(),
                    interval = 0,
                    easeFactor = 2.5f,
                    consecutiveCorrect = 0
                )
            ).toInt()

            resolvedMoves.forEachIndexed { moveIndex, rm ->
                repertoireDao.insertLineMove(
                    LineMove(
                        lineId = lineId, moveIndex = moveIndex, moveSan = rm.san, fen = rm.fen, comment = rm.comment, arrows = null
                    )
                )
            }

            insertedLinesCount++
            currentLineNumber++
        }

        return insertedLinesCount
    }


    internal data class ParsedMove(val san: String, val comment: String?)

    private data class ResolvedMove(val san: String, val fen: String, val comment: String?)

    /**
     * Wynik pojedynczego wywołania rekurencyjnego parsera movetextu.
     */
    internal data class ParseResult(
        val lines: List<List<ParsedMove>>, val nextIndex: Int
    )

    /**
     * Publiczna (wewnątrz klasy) funkcja pomocnicza – zwraca wszystkie linie
     * wygenerowane z ciągu movetext (główna + wszystkie warianty).
     */
    internal fun parseLinesFromMovetext(text: String): List<List<ParsedMove>> {
        val (lines, _) = parseMovetextRecursive(text, 0, emptyList())
        return lines
    }

    /**
     * Rekurencyjnie przegląda ciąg movetext (znaki), respektując nawiasy okrągłe
     * jako warianty oraz klamry jako komentarze i buduje wszystkie ścieżki od
     * początku partii do liści drzewa wariantów.
     */
    internal fun parseMovetextRecursive(
        text: String, startIndex: Int, parentPrefix: List<ParsedMove>
    ): ParseResult {
        // "current" holds the main line for this subtree. Any variations
        // encountered along the way are collected separately in
        // variationLines so that when we return from this call we can place
        // the main line FIRST, followed by its variations. This preserves
        // the intuitive ordering where the main branch of a subtree comes
        // before its side branches.
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
                c.isWhitespace() -> {
                    i++
                }

                // Komentarz w klamrach: { ... }
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

                // Dowolny blok w nawiasach kwadratowych (np. pozostałości [%...]) – ignorujemy
                c == '[' -> {
                    var j = i + 1
                    while (j < length && text[j] != ']') {
                        j++
                    }
                    i = if (j < length) j + 1 else j
                }

                // Początek wariantu w nawiasach okrągłych
                c == '(' -> {
                    // Wariant jest alternatywą dla OSTATNIEGO ruchu w "current",
                    // więc jako prefiks używamy wszystkich ruchów oprócz ostatniego.
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

                // Koniec wariantu
                c == ')' -> {
                    i++
                    break
                }

                // Wynik partii – kończymy parsowanie
                text.startsWith("1-0", i) || text.startsWith("0-1", i) || text.startsWith("1/2-1/2", i) || c == '*' -> {
                    // Konsumujemy wynik i kończymy tę gałąź
                    i += when {
                        text.startsWith("1-0", i) -> 3
                        text.startsWith("0-1", i) -> 3
                        text.startsWith("1/2-1/2", i) -> "1/2-1/2".length
                        else -> 1 // '*'
                    }
                    break
                }

                // NAG: $x – ignorujemy
                c == '$' -> {
                    var j = i + 1
                    while (j < length && text[j].isDigit()) {
                        j++
                    }
                    i = j
                }

                // Numer posunięcia: np. "1.", "12..." – ignorujemy
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
                    // SAN ruchu – czytamy do białego znaku lub nawiasu/klamry
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

        // Build the final list for this subtree: main line first (if any),
        // then all collected variation lines.
        val allLines = mutableListOf<List<ParsedMove>>()
        if (current.isNotEmpty()) {
            allLines.add(current.toList())
        }
        allLines.addAll(variationLines)

        return ParseResult(allLines, i)
    }

    companion object {
        private val TAG_REGEX = Regex("""\[(\w+)\s+"(.*)"]""")

        // Minimalna liczba ruchów (plies) w linii, aby uznać ją za sensowną do zapisania.
        // Zbyt krótkie linie zwykle wynikają z przerwanego wariantu lub błędu dopasowania SAN.
        private const val MIN_MOVES_PER_LINE = 4
    }
}

package com.example.chessrepertoiretrainer.core.opening

object OpeningClassifier {

    private val ECO_HEADER_REGEX = Regex("""^\[ECO\s+"([A-E]\d{2})"\]""", RegexOption.MULTILINE)

    fun classify(pgn: String, registry: OpeningRegistry): OpeningEntry? {
        val ecoMatch = ECO_HEADER_REGEX.find(pgn)
        if (ecoMatch != null) {
            val eco = ecoMatch.groupValues[1]
            registry.lookupByEco(eco)?.let { return it }
        }
        return null
    }

    fun classifyByFenHistory(fenHistory: List<String>, registry: OpeningRegistry): OpeningEntry? {
        // Walk from deepest position back to find the most specific opening match
        for (fen in fenHistory.asReversed()) {
            val normalized = normalizeFen(fen)
            registry.lookupByFen(normalized)?.let { return it }
        }
        return null
    }

    fun normalizeFen(fen: String): String = fen.split(" ").take(4).joinToString(" ")
}

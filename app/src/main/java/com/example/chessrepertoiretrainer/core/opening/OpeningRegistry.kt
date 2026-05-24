package com.example.chessrepertoiretrainer.core.opening

import android.content.res.AssetManager
import org.json.JSONArray

class OpeningRegistry(assets: AssetManager) {

    private val entries: List<OpeningEntry>
    val fenToEntry: Map<String, OpeningEntry>
    val ecoToEntry: Map<String, OpeningEntry>

    init {
        val json = assets.open("eco_openings.json").bufferedReader().readText()
        val array = JSONArray(json)
        val list = ArrayList<OpeningEntry>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                OpeningEntry(
                    eco = obj.getString("eco"),
                    name = obj.getString("name"),
                    fen = obj.getString("fen"),
                    family = obj.getString("family")
                )
            )
        }
        entries = list
        // Last entry per FEN wins (entries are ordered from general to specific, so specific wins)
        fenToEntry = entries.associateBy { it.fen }
        // Last entry per ECO wins (keeps deepest/most-specific variation for each ECO code)
        ecoToEntry = entries.associateBy { it.eco }
    }

    fun lookupByFen(normalizedFen: String): OpeningEntry? = fenToEntry[normalizedFen]
    fun lookupByEco(eco: String): OpeningEntry? = ecoToEntry[eco]
}

package com.example.chessrepertoiretrainer.feature.repertoire.data.transfer

import com.example.chessrepertoiretrainer.core.database.dao.RepertoireDao
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RepertoireExporter(
    private val repertoireDao: RepertoireDao,
    private val json: Json = DefaultJson,
) {
    suspend fun export(repertoireIds: List<Int>): String {
        val repertoires = repertoireIds.mapNotNull { id ->
            val rep = repertoireDao.getRepertoireById(id) ?: return@mapNotNull null
            val chapters = repertoireDao.getChaptersForRepertoire(rep.id).first().map { chapter ->
                val lines = repertoireDao.getLinesForChapter(chapter.id).first().map { line ->
                    val moves = repertoireDao.getMovesForLine(line.id).first().map { mv ->
                        LineMoveDto(
                            moveIndex = mv.moveIndex,
                            moveSan = mv.moveSan,
                            fen = mv.fen,
                            comment = mv.comment,
                            arrows = mv.arrows,
                        )
                    }
                    LineDto(
                        name = line.name,
                        sortOrder = line.sortOrder,
                        lastEcoCode = line.lastEcoCode,
                        moves = moves,
                    )
                }
                ChapterDto(
                    name = chapter.name,
                    sortOrder = chapter.sortOrder,
                    primaryEcoCode = chapter.primaryEcoCode,
                    lines = lines,
                )
            }
            RepertoireDto(name = rep.name, color = rep.color, chapters = chapters)
        }
        return json.encodeToString(TransferEnvelope(repertoires = repertoires))
    }
}

internal val DefaultJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

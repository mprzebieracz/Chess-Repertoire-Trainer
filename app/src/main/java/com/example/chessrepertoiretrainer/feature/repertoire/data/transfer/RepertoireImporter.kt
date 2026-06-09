package com.example.chessrepertoiretrainer.feature.repertoire.data.transfer

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.example.chessrepertoiretrainer.core.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.core.database.entity.Chapter
import com.example.chessrepertoiretrainer.core.database.entity.Line
import com.example.chessrepertoiretrainer.core.database.entity.LineMove
import com.example.chessrepertoiretrainer.core.database.entity.Repertoire
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

data class ImportResult(val imported: Int, val importedNames: List<String>)

class RepertoireImporter(
    private val repertoireDao: RepertoireDao,
    private val database: RoomDatabase,
    private val json: Json = DefaultJson,
) {
    fun parsePreview(jsonString: String): TransferEnvelope =
        json.decodeFromString(TransferEnvelope.serializer(), jsonString)

    suspend fun import(envelope: TransferEnvelope): ImportResult {
        // Read existing names before the transaction so we can compute unique names.
        val existingNames = repertoireDao.getAllRepertoires().first()
            .map { it.name }
            .toMutableSet()

        val importedNames = mutableListOf<String>()

        // All inserts run in a single transaction so Room only fires one observer
        // notification after everything is committed — preventing a partial line count
        // from showing up in the UI mid-import.
        database.withTransaction {
            for (rep in envelope.repertoires) {
                val finalName = uniqueNameFor(rep.name, existingNames)
                existingNames.add(finalName)

                val newRepertoireId = repertoireDao.insertRepertoire(
                    Repertoire(name = finalName, color = rep.color)
                ).toInt()

                for (chapter in rep.chapters) {
                    val newChapterId = repertoireDao.insertChapter(
                        Chapter(
                            repertoireId = newRepertoireId,
                            name = chapter.name,
                            sortOrder = chapter.sortOrder,
                            primaryEcoCode = chapter.primaryEcoCode,
                        )
                    ).toInt()

                    for (line in chapter.lines) {
                        val newLineId = repertoireDao.insertLine(
                            Line(
                                chapterId = newChapterId,
                                name = line.name,
                                nextReviewDate = 0L,
                                interval = 0,
                                easeFactor = 2.5f,
                                consecutiveCorrect = 0,
                                isLearned = false,
                                learnedAt = null,
                                timesTrained = 0,
                                lastTrainedAt = null,
                                sortOrder = line.sortOrder,
                                lastEcoCode = line.lastEcoCode,
                            )
                        ).toInt()

                        for (move in line.moves) {
                            repertoireDao.insertLineMove(
                                LineMove(
                                    lineId = newLineId,
                                    moveIndex = move.moveIndex,
                                    moveSan = move.moveSan,
                                    fen = move.fen,
                                    comment = move.comment,
                                    arrows = move.arrows,
                                )
                            )
                        }
                    }
                }
                importedNames.add(finalName)
            }
        }

        return ImportResult(imported = importedNames.size, importedNames = importedNames)
    }

    private fun uniqueNameFor(base: String, taken: Set<String>): String {
        if (base !in taken) return base
        val firstAttempt = "$base (imported)"
        if (firstAttempt !in taken) return firstAttempt
        var n = 2
        while (true) {
            val candidate = "$base (imported $n)"
            if (candidate !in taken) return candidate
            n++
        }
    }
}

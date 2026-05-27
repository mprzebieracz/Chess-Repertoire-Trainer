package com.example.chessrepertoiretrainer.core.activity

import com.example.chessrepertoiretrainer.core.database.dao.DailyActivityDao
import com.example.chessrepertoiretrainer.core.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.core.database.dao.ReviewLogDao
import com.example.chessrepertoiretrainer.core.database.entity.DailyActivity
import com.example.chessrepertoiretrainer.core.database.entity.Line
import com.example.chessrepertoiretrainer.core.database.entity.ReviewLog
import kotlin.math.roundToInt

class ActivityRecorder(
    private val dailyActivityDao: DailyActivityDao,
    private val reviewLogDao: ReviewLogDao,
    private val repertoireDao: RepertoireDao
) {

    suspend fun recordLineReviewed(line: Line, wasCorrect: Boolean, msTaken: Long? = null): Line {
        val updated = applySpacedRepetition(line, wasCorrect)
        repertoireDao.updateLine(updated)
        reviewLogDao.insertLog(
            ReviewLog(
                lineId = line.id,
                reviewedAt = System.currentTimeMillis(),
                grade = if (wasCorrect) 3 else 0,
                wasCorrect = wasCorrect,
                msTaken = msTaken
            )
        )
        incrementToday { it.copy(linesTrained = it.linesTrained + 1) }
        return updated
    }

    suspend fun recordPuzzleSolved() = incrementToday { it.copy(puzzlesSolved = it.puzzlesSolved + 1) }

    suspend fun recordGamesImported(count: Int) =
        incrementToday { it.copy(gamesImported = it.gamesImported + count) }

    suspend fun getActivities(fromMs: Long, toMs: Long): List<DailyActivity> =
        dailyActivityDao.getAll().filter { it.date in fromMs..toMs }

    suspend fun currentStreakDays(): Int {
        val activities = dailyActivityDao.getAll()
        var streak = 0
        var expectedDay = todayMs()
        for (a in activities) {
            when {
                a.date == expectedDay && hasActivity(a) -> {
                    streak++
                    expectedDay -= MILLIS_PER_DAY
                }
                a.date == expectedDay -> expectedDay -= MILLIS_PER_DAY // inactive day
                else -> break
            }
        }
        return streak
    }

    private fun applySpacedRepetition(line: Line, wasCorrect: Boolean): Line {
        val newConsecutive = if (wasCorrect) line.consecutiveCorrect + 1 else 0
        val newInterval = when {
            !wasCorrect -> 1
            line.consecutiveCorrect == 0 -> 1
            line.consecutiveCorrect == 1 -> 6
            else -> (line.interval * line.easeFactor).roundToInt().coerceAtLeast(1)
        }
        val newEaseFactor = if (wasCorrect) {
            // SM-2 formula with grade = 5 for correct
            (line.easeFactor + 0.1f).coerceAtLeast(1.3f)
        } else {
            line.easeFactor // easeFactor unchanged on failure
        }
        val now = System.currentTimeMillis()
        return line.copy(
            interval = newInterval,
            easeFactor = newEaseFactor,
            consecutiveCorrect = newConsecutive,
            nextReviewDate = now + newInterval * MILLIS_PER_DAY,
            isLearned = wasCorrect || line.isLearned,
            learnedAt = if (wasCorrect && !line.isLearned) now else line.learnedAt,
            timesTrained = line.timesTrained + 1,
            lastTrainedAt = now
        )
    }

    private suspend fun incrementToday(update: (DailyActivity) -> DailyActivity) {
        val today = todayMs()
        val now = System.currentTimeMillis()
        val existing = dailyActivityDao.getByDate(today)
        val updated = if (existing != null) {
            update(existing).copy(lastUpdatedAt = now)
        } else {
            update(DailyActivity(date = today, lastUpdatedAt = now)).copy(lastUpdatedAt = now)
        }
        dailyActivityDao.upsertActivity(updated)
    }

    private fun hasActivity(a: DailyActivity) = a.linesTrained > 0 || a.puzzlesSolved > 0

    companion object {
        private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L

        fun todayMs(): Long {
            val now = System.currentTimeMillis()
            return now - (now % MILLIS_PER_DAY)
        }
    }
}

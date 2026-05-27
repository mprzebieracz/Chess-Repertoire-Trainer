package com.example.chessrepertoiretrainer.core.activity

import com.example.chessrepertoiretrainer.core.database.dao.DailyActivityDao
import com.example.chessrepertoiretrainer.core.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.core.database.dao.ReviewLogDao
import com.example.chessrepertoiretrainer.core.database.entity.DailyActivity
import com.example.chessrepertoiretrainer.core.database.entity.Line
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ActivityRecorderTest {

    private lateinit var activityDao: DailyActivityDao
    private lateinit var reviewLogDao: ReviewLogDao
    private lateinit var repertoireDao: RepertoireDao
    private lateinit var recorder: ActivityRecorder

    private val DAY = 24 * 60 * 60 * 1000L

    @Before
    fun setUp() {
        activityDao = mockk(relaxed = true)
        reviewLogDao = mockk(relaxed = true)
        repertoireDao = mockk(relaxed = true)
        recorder = ActivityRecorder(activityDao, reviewLogDao, repertoireDao)
    }

    private fun makeLine(
        id: Int = 1,
        interval: Int = 0,
        easeFactor: Float = 2.5f,
        consecutiveCorrect: Int = 0,
        isLearned: Boolean = false,
        learnedAt: Long? = null,
        timesTrained: Int = 0
    ) = Line(
        id = id,
        chapterId = 1,
        name = "Test",
        nextReviewDate = 0L,
        interval = interval,
        easeFactor = easeFactor,
        consecutiveCorrect = consecutiveCorrect,
        isLearned = isLearned,
        learnedAt = learnedAt,
        timesTrained = timesTrained
    )

    // ---- SM-2: interval logic ----

    @Test
    fun `incorrect on first attempt - interval becomes 1`() = runTest {
        coEvery { activityDao.getByDate(any()) } returns null
        val line = makeLine(consecutiveCorrect = 0, interval = 0)
        val result = recorder.recordLineReviewed(line, wasCorrect = false)
        assertEquals(1, result.interval)
    }

    @Test
    fun `first correct attempt - interval stays 1`() = runTest {
        coEvery { activityDao.getByDate(any()) } returns null
        val line = makeLine(consecutiveCorrect = 0, interval = 0)
        val result = recorder.recordLineReviewed(line, wasCorrect = true)
        assertEquals(1, result.interval)
    }

    @Test
    fun `second correct attempt - interval becomes 6`() = runTest {
        coEvery { activityDao.getByDate(any()) } returns null
        val line = makeLine(consecutiveCorrect = 1, interval = 1)
        val result = recorder.recordLineReviewed(line, wasCorrect = true)
        assertEquals(6, result.interval)
    }

    @Test
    fun `third correct attempt - interval multiplied by easeFactor`() = runTest {
        coEvery { activityDao.getByDate(any()) } returns null
        val line = makeLine(consecutiveCorrect = 2, interval = 6, easeFactor = 2.5f)
        val result = recorder.recordLineReviewed(line, wasCorrect = true)
        val expected = (6 * 2.5f).toInt()  // 15
        assertEquals(expected, result.interval)
    }

    @Test
    fun `incorrect after streak - interval resets to 1`() = runTest {
        coEvery { activityDao.getByDate(any()) } returns null
        val line = makeLine(consecutiveCorrect = 5, interval = 30)
        val result = recorder.recordLineReviewed(line, wasCorrect = false)
        assertEquals(1, result.interval)
    }

    @Test
    fun `interval is at least 1 after SM-2 multiplication`() = runTest {
        coEvery { activityDao.getByDate(any()) } returns null
        val line = makeLine(consecutiveCorrect = 2, interval = 0, easeFactor = 1.3f)
        val result = recorder.recordLineReviewed(line, wasCorrect = true)
        assertTrue(result.interval >= 1)
    }

    // ---- SM-2: consecutiveCorrect ----

    @Test
    fun `consecutive correct increments on success`() = runTest {
        coEvery { activityDao.getByDate(any()) } returns null
        val line = makeLine(consecutiveCorrect = 2)
        val result = recorder.recordLineReviewed(line, wasCorrect = true)
        assertEquals(3, result.consecutiveCorrect)
    }

    @Test
    fun `consecutive correct resets to 0 on failure`() = runTest {
        coEvery { activityDao.getByDate(any()) } returns null
        val line = makeLine(consecutiveCorrect = 5)
        val result = recorder.recordLineReviewed(line, wasCorrect = false)
        assertEquals(0, result.consecutiveCorrect)
    }

    // ---- SM-2: easeFactor ----

    @Test
    fun `easeFactor increments by one tenth on correct`() = runTest {
        coEvery { activityDao.getByDate(any()) } returns null
        val line = makeLine(easeFactor = 2.5f, consecutiveCorrect = 2)
        val result = recorder.recordLineReviewed(line, wasCorrect = true)
        assertEquals(2.6f, result.easeFactor, 0.001f)
    }

    @Test
    fun `easeFactor unchanged on incorrect`() = runTest {
        coEvery { activityDao.getByDate(any()) } returns null
        val line = makeLine(easeFactor = 2.5f, consecutiveCorrect = 3)
        val result = recorder.recordLineReviewed(line, wasCorrect = false)
        assertEquals(2.5f, result.easeFactor, 0.001f)
    }

    @Test
    fun `easeFactor does not go below 1-3 minimum`() = runTest {
        coEvery { activityDao.getByDate(any()) } returns null
        // easeFactor at minimum but correct → should go to 1.4, not below 1.3
        val line = makeLine(easeFactor = 1.3f, consecutiveCorrect = 2)
        val result = recorder.recordLineReviewed(line, wasCorrect = true)
        assertTrue(result.easeFactor >= 1.3f)
    }

    // ---- SM-2: isLearned + learnedAt ----

    @Test
    fun `isLearned set to true on first correct attempt`() = runTest {
        coEvery { activityDao.getByDate(any()) } returns null
        val line = makeLine(isLearned = false)
        val result = recorder.recordLineReviewed(line, wasCorrect = true)
        assertTrue(result.isLearned)
    }

    @Test
    fun `learnedAt set on first time becoming learned`() = runTest {
        coEvery { activityDao.getByDate(any()) } returns null
        val line = makeLine(isLearned = false, learnedAt = null)
        val result = recorder.recordLineReviewed(line, wasCorrect = true)
        assertNotNull(result.learnedAt)
    }

    @Test
    fun `isLearned stays true even after incorrect`() = runTest {
        coEvery { activityDao.getByDate(any()) } returns null
        val line = makeLine(isLearned = true)
        val result = recorder.recordLineReviewed(line, wasCorrect = false)
        assertTrue(result.isLearned)
    }

    @Test
    fun `learnedAt not overwritten when already learned`() = runTest {
        coEvery { activityDao.getByDate(any()) } returns null
        val originalLearnedAt = 123456789L
        val line = makeLine(isLearned = true, learnedAt = originalLearnedAt)
        val result = recorder.recordLineReviewed(line, wasCorrect = true)
        assertEquals(originalLearnedAt, result.learnedAt)
    }

    // ---- SM-2: timesTrained ----

    @Test
    fun `timesTrained increments by 1 on correct`() = runTest {
        coEvery { activityDao.getByDate(any()) } returns null
        val line = makeLine(timesTrained = 3)
        val result = recorder.recordLineReviewed(line, wasCorrect = true)
        assertEquals(4, result.timesTrained)
    }

    @Test
    fun `timesTrained increments by 1 on incorrect`() = runTest {
        coEvery { activityDao.getByDate(any()) } returns null
        val line = makeLine(timesTrained = 7)
        val result = recorder.recordLineReviewed(line, wasCorrect = false)
        assertEquals(8, result.timesTrained)
    }

    // ---- nextReviewDate ----

    @Test
    fun `nextReviewDate set to future after interval days`() = runTest {
        coEvery { activityDao.getByDate(any()) } returns null
        val before = System.currentTimeMillis()
        val line = makeLine(consecutiveCorrect = 0)
        val result = recorder.recordLineReviewed(line, wasCorrect = true)
        val after = System.currentTimeMillis()
        assertTrue(result.nextReviewDate >= before + 1 * DAY)
        assertTrue(result.nextReviewDate <= after + 1 * DAY + 1000L)
    }

    // ---- currentStreakDays ----

    @Test
    fun `streak is 0 with no activities`() = runTest {
        coEvery { activityDao.getAll() } returns emptyList()
        assertEquals(0, recorder.currentStreakDays())
    }

    @Test
    fun `streak is 1 when today is active`() = runTest {
        val today = ActivityRecorder.todayMs()
        val activity = DailyActivity(date = today, linesTrained = 1, lastUpdatedAt = today)
        coEvery { activityDao.getAll() } returns listOf(activity)
        assertEquals(1, recorder.currentStreakDays())
    }

    @Test
    fun `streak is 2 for today and yesterday both active`() = runTest {
        val today = ActivityRecorder.todayMs()
        val yesterday = today - DAY
        val activities = listOf(
            DailyActivity(date = today, linesTrained = 1, lastUpdatedAt = today),
            DailyActivity(date = yesterday, linesTrained = 1, lastUpdatedAt = yesterday)
        )
        coEvery { activityDao.getAll() } returns activities
        assertEquals(2, recorder.currentStreakDays())
    }

    @Test
    fun `inactive day (zero activity) is skipped but does not break streak`() = runTest {
        val today = ActivityRecorder.todayMs()
        val yesterday = today - DAY
        val twoDaysAgo = today - 2 * DAY
        val activities = listOf(
            DailyActivity(date = today, linesTrained = 1, lastUpdatedAt = today),
            DailyActivity(date = yesterday, linesTrained = 0, puzzlesSolved = 0, lastUpdatedAt = yesterday),
            DailyActivity(date = twoDaysAgo, linesTrained = 1, lastUpdatedAt = twoDaysAgo)
        )
        coEvery { activityDao.getAll() } returns activities
        // Inactive days are skipped (don't break streak) but don't count toward the total
        assertEquals(2, recorder.currentStreakDays())
    }

    @Test
    fun `gap in activities breaks streak`() = runTest {
        val today = ActivityRecorder.todayMs()
        val twoDaysAgo = today - 2 * DAY
        val activities = listOf(
            DailyActivity(date = today, linesTrained = 1, lastUpdatedAt = today),
            DailyActivity(date = twoDaysAgo, linesTrained = 1, lastUpdatedAt = twoDaysAgo)
        )
        coEvery { activityDao.getAll() } returns activities
        assertEquals(1, recorder.currentStreakDays())
    }

    @Test
    fun `streak counts consecutive active days`() = runTest {
        val today = ActivityRecorder.todayMs()
        val activities = (0L..4L).map { offset ->
            DailyActivity(date = today - offset * DAY, linesTrained = 1, lastUpdatedAt = today)
        }
        coEvery { activityDao.getAll() } returns activities
        assertEquals(5, recorder.currentStreakDays())
    }

    @Test
    fun `puzzle solved counts as activity for streak`() = runTest {
        val today = ActivityRecorder.todayMs()
        val activity = DailyActivity(date = today, linesTrained = 0, puzzlesSolved = 1, lastUpdatedAt = today)
        coEvery { activityDao.getAll() } returns listOf(activity)
        assertEquals(1, recorder.currentStreakDays())
    }

    // ---- getActivities range filter ----

    @Test
    fun `getActivities returns only activities within range`() = runTest {
        val today = ActivityRecorder.todayMs()
        val yesterday = today - DAY
        val weekAgo = today - 7 * DAY
        val allActivities = listOf(
            DailyActivity(date = today, linesTrained = 1, lastUpdatedAt = today),
            DailyActivity(date = yesterday, linesTrained = 1, lastUpdatedAt = yesterday),
            DailyActivity(date = weekAgo, linesTrained = 1, lastUpdatedAt = weekAgo)
        )
        coEvery { activityDao.getAll() } returns allActivities
        val result = recorder.getActivities(yesterday, today)
        assertEquals(2, result.size)
        assertTrue(result.none { it.date == weekAgo })
    }

    @Test
    fun `getActivities with empty range returns empty`() = runTest {
        coEvery { activityDao.getAll() } returns emptyList()
        val result = recorder.getActivities(0L, 1000L)
        assertTrue(result.isEmpty())
    }

    // ---- repository side effect: update called ----

    @Test
    fun `recordLineReviewed updates the line in the repertoire dao`() = runTest {
        coEvery { activityDao.getByDate(any()) } returns null
        val line = makeLine()
        recorder.recordLineReviewed(line, wasCorrect = true)
        coVerify { repertoireDao.updateLine(any()) }
    }
}

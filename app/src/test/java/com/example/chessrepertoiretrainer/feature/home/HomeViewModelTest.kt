package com.example.chessrepertoiretrainer.feature.home

import com.example.chessrepertoiretrainer.core.activity.ActivityRecorder
import com.example.chessrepertoiretrainer.core.database.entity.Puzzle
import com.example.chessrepertoiretrainer.feature.puzzles.PuzzleRepository
import com.example.chessrepertoiretrainer.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val puzzleRepository: PuzzleRepository = mockk(relaxed = true)
    private val activityRecorder: ActivityRecorder = mockk(relaxed = true)

    private fun makePuzzle(
        isSolved: Boolean = false,
        rating: Int = 1500,
        themes: String = "fork"
    ) = Puzzle(
        id = "test123",
        fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        moves = "e2e4",
        rating = rating,
        themes = themes,
        isSolved = isSolved,
        attempts = 0,
        sourceDate = "2026-05-27"
    )

    private fun makeVm() = HomeViewModel(puzzleRepository, activityRecorder)

    // ---- puzzle state transitions ----

    @Test
    fun `unsolved puzzle in db - state becomes Available`() = runTest {
        coEvery { puzzleRepository.getTodaysPuzzle() } returns makePuzzle(
            isSolved = false,
            rating = 1500,
            themes = "fork mateIn2"
        )
        val vm = makeVm()
        val state = vm.uiState.value.dailyPuzzleState
        assertTrue(state is DailyPuzzleState.Available)
        assertEquals(1500, (state as DailyPuzzleState.Available).rating)
        assertEquals("fork mateIn2", state.themes)
    }

    @Test
    fun `solved puzzle in db - state becomes Solved`() = runTest {
        coEvery { puzzleRepository.getTodaysPuzzle() } returns makePuzzle(isSolved = true)
        val vm = makeVm()
        assertEquals(DailyPuzzleState.Solved, vm.uiState.value.dailyPuzzleState)
    }

    @Test
    fun `no puzzle in db but fetch succeeds - state becomes Available`() = runTest {
        coEvery { puzzleRepository.getTodaysPuzzle() } returns null
        coEvery { puzzleRepository.fetchAndSaveDailyPuzzle() } returns makePuzzle(
            rating = 1800,
            themes = "pin"
        )
        val vm = makeVm()
        val state = vm.uiState.value.dailyPuzzleState
        assertTrue(state is DailyPuzzleState.Available)
        assertEquals(1800, (state as DailyPuzzleState.Available).rating)
    }

    @Test
    fun `no puzzle in db and fetch returns null - state is Error`() = runTest {
        coEvery { puzzleRepository.getTodaysPuzzle() } returns null
        coEvery { puzzleRepository.fetchAndSaveDailyPuzzle() } returns null
        val vm = makeVm()
        assertTrue(vm.uiState.value.dailyPuzzleState is DailyPuzzleState.Error)
    }

    @Test
    fun `getTodaysPuzzle throws - state is Error with exception message`() = runTest {
        coEvery { puzzleRepository.getTodaysPuzzle() } throws RuntimeException("Network error")
        val vm = makeVm()
        val state = vm.uiState.value.dailyPuzzleState
        assertTrue(state is DailyPuzzleState.Error)
        assertEquals("Network error", (state as DailyPuzzleState.Error).message)
    }

    @Test
    fun `fetchAndSaveDailyPuzzle throws - state is Error`() = runTest {
        coEvery { puzzleRepository.getTodaysPuzzle() } returns null
        coEvery { puzzleRepository.fetchAndSaveDailyPuzzle() } throws RuntimeException("Timeout")
        val vm = makeVm()
        assertTrue(vm.uiState.value.dailyPuzzleState is DailyPuzzleState.Error)
    }

    // ---- streak ----

    @Test
    fun `streak from activityRecorder is reflected in uiState`() = runTest {
        coEvery { puzzleRepository.getTodaysPuzzle() } returns makePuzzle()
        coEvery { activityRecorder.currentStreakDays() } returns 7
        coEvery { activityRecorder.getActivities(any(), any()) } returns emptyList()
        val vm = makeVm()
        assertEquals(7, vm.uiState.value.streak)
    }

    @Test
    fun `zero streak is correctly reflected`() = runTest {
        coEvery { puzzleRepository.getTodaysPuzzle() } returns makePuzzle()
        coEvery { activityRecorder.currentStreakDays() } returns 0
        val vm = makeVm()
        assertEquals(0, vm.uiState.value.streak)
    }

    // ---- retry ----

    @Test
    fun `retry after error re-triggers load and recovers`() = runTest {
        coEvery { puzzleRepository.getTodaysPuzzle() } returns null
        coEvery { puzzleRepository.fetchAndSaveDailyPuzzle() } returns null
        val vm = makeVm()
        assertTrue(vm.uiState.value.dailyPuzzleState is DailyPuzzleState.Error)

        // Re-stub to return a puzzle
        coEvery { puzzleRepository.getTodaysPuzzle() } returns makePuzzle(rating = 2000)
        vm.retry()

        assertTrue(vm.uiState.value.dailyPuzzleState is DailyPuzzleState.Available)
        assertEquals(2000, (vm.uiState.value.dailyPuzzleState as DailyPuzzleState.Available).rating)
    }

    @Test
    fun `retry after error can result in another error`() = runTest {
        coEvery { puzzleRepository.getTodaysPuzzle() } returns null
        coEvery { puzzleRepository.fetchAndSaveDailyPuzzle() } returns null
        val vm = makeVm()
        assertTrue(vm.uiState.value.dailyPuzzleState is DailyPuzzleState.Error)

        vm.retry()

        assertTrue(vm.uiState.value.dailyPuzzleState is DailyPuzzleState.Error)
    }
}
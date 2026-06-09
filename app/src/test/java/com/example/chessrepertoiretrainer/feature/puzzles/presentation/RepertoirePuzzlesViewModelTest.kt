package com.example.chessrepertoiretrainer.feature.puzzles.presentation

import com.example.chessrepertoiretrainer.core.database.entity.RepertoireOpening
import com.example.chessrepertoiretrainer.core.opening.OpeningRegistry
import com.example.chessrepertoiretrainer.feature.puzzles.PuzzleRepository
import com.example.chessrepertoiretrainer.feature.repertoire.domain.RepertoireRepository
import com.example.chessrepertoiretrainer.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RepertoirePuzzlesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val puzzleRepository: PuzzleRepository = mockk(relaxed = true)
    private val repertoireRepository: RepertoireRepository = mockk(relaxed = true)
    private val openingRegistry: OpeningRegistry = mockk(relaxed = true)

    private val singleOpening = listOf(RepertoireOpening(family = "Sicilian Defense", eco = "B20"))
    private val twoOpenings = listOf(
        RepertoireOpening(family = "Sicilian Defense", eco = "B20"),
        RepertoireOpening(family = "King's Indian", eco = "E60"),
    )

    private fun makeVm(
        openings: List<RepertoireOpening> = singleOpening,
        // specific counts registered AFTER any() so they take priority in MockK
        unsolvedCounts: Map<String, Int> = emptyMap()
    ): RepertoirePuzzlesViewModel {
        coEvery { puzzleRepository.getRepertoireOpenings() } returns openings
        coEvery { puzzleRepository.countUnsolvedForFamily(any()) } returns 5
        for ((family, count) in unsolvedCounts) {
            coEvery { puzzleRepository.countUnsolvedForFamily(family) } returns count
        }
        coEvery { repertoireRepository.getAllLineMoveFens() } returns emptyMap()
        return RepertoirePuzzlesViewModel(puzzleRepository, repertoireRepository, openingRegistry)
    }

    // ---- initial load ----

    @Test
    fun `isLoading false after init completes`() = runTest {
        val vm = makeVm()
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `openings from repository populated after init`() = runTest {
        val vm = makeVm(singleOpening)
        assertEquals(1, vm.uiState.value.openings.size)
        assertEquals("Sicilian Defense", vm.uiState.value.openings[0].family)
    }

    @Test
    fun `unsolvedCount from repository shown in opening items`() = runTest {
        val vm = makeVm(unsolvedCounts = mapOf("Sicilian Defense" to 12))
        assertEquals(12, vm.uiState.value.openings[0].unsolvedCount)
    }

    // ---- toggleSelection ----

    @Test
    fun `toggleSelection on unselected item marks it selected`() = runTest {
        val vm = makeVm()
        assertFalse(vm.uiState.value.openings[0].isSelected)
        vm.toggleSelection("Sicilian Defense")
        assertTrue(vm.uiState.value.openings[0].isSelected)
    }

    @Test
    fun `toggleSelection on selected item deselects it`() = runTest {
        val vm = makeVm()
        vm.toggleSelection("Sicilian Defense")
        assertTrue(vm.uiState.value.openings[0].isSelected)
        vm.toggleSelection("Sicilian Defense")
        assertFalse(vm.uiState.value.openings[0].isSelected)
    }

    @Test
    fun `toggleSelection unknown family leaves all items unchanged`() = runTest {
        val vm = makeVm()
        vm.toggleSelection("Unknown Opening")
        assertTrue(vm.uiState.value.openings.none { it.isSelected })
    }

    // ---- selectedFamilies ----

    @Test
    fun `selectedFamilies returns only selected family names`() = runTest {
        val vm = makeVm(twoOpenings)
        vm.toggleSelection("Sicilian Defense")
        val selected = vm.selectedFamilies()
        assertEquals(listOf("Sicilian Defense"), selected)
    }

    @Test
    fun `selectedFamilies returns empty list when nothing selected`() = runTest {
        val vm = makeVm()
        assertTrue(vm.selectedFamilies().isEmpty())
    }

    @Test
    fun `selectedFamilies returns all when all selected`() = runTest {
        val vm = makeVm(twoOpenings)
        vm.toggleSelection("Sicilian Defense")
        vm.toggleSelection("King's Indian")
        assertEquals(2, vm.selectedFamilies().size)
    }

    // ---- totalUnsolvedInSelection ----

    @Test
    fun `totalUnsolvedInSelection is 0 when nothing selected`() = runTest {
        val vm = makeVm()
        assertEquals(0, vm.totalUnsolvedInSelection())
    }

    @Test
    fun `totalUnsolvedInSelection sums unsolvedCounts of selected openings`() = runTest {
        val vm = makeVm(
            openings = twoOpenings,
            unsolvedCounts = mapOf("Sicilian Defense" to 10, "King's Indian" to 7)
        )
        vm.toggleSelection("Sicilian Defense")
        vm.toggleSelection("King's Indian")
        assertEquals(17, vm.totalUnsolvedInSelection())
    }

    @Test
    fun `totalUnsolvedInSelection only includes selected families`() = runTest {
        val vm = makeVm(
            openings = twoOpenings,
            unsolvedCounts = mapOf("Sicilian Defense" to 10, "King's Indian" to 7)
        )
        vm.toggleSelection("Sicilian Defense")
        // King's Indian not selected
        assertEquals(10, vm.totalUnsolvedInSelection())
    }

    // ---- selections preserved after manualRefresh ----

    @Test
    fun `selections preserved after manualRefresh`() = runTest {
        val vm = makeVm(singleOpening)
        vm.toggleSelection("Sicilian Defense")
        assertTrue(vm.uiState.value.openings[0].isSelected)

        vm.manualRefresh()

        // After re-scan with the same openings, selection should be preserved
        assertTrue(vm.uiState.value.openings.any { it.family == "Sicilian Defense" && it.isSelected })
    }
}
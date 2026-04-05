package com.example.chessrepertoiretrainer.ui.viewmodels

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningTreeViewModelTest {

    @Test
    fun matchesTimeControlFilter_noSelection_acceptsAll() {
        val selected = emptySet<String>()
        // When no categories are selected, we do not filter on time control.
        assertTrue(matchesTimeControlFilter(null, selected))
        assertTrue(matchesTimeControlFilter("bullet", selected))
        assertTrue(matchesTimeControlFilter("rapid", selected))
    }

    @Test
    fun matchesTimeControlFilter_singleCategory() {
        val selected = setOf("blitz")
        assertTrue(matchesTimeControlFilter("blitz", selected))
        // Rapid should not match when only blitz is selected.
        assertFalse(matchesTimeControlFilter("rapid", selected))
        // Missing category should not match.
        assertFalse(matchesTimeControlFilter(null, selected))
    }

    @Test
    fun matchesTimeControlFilter_multipleCategoriesOrLogic() {
        val selected = setOf("blitz", "rapid")
        // Blitz game
        assertTrue(matchesTimeControlFilter("blitz", selected))
        // Rapid game
        assertTrue(matchesTimeControlFilter("rapid", selected))
        // Bullet should not match
        assertFalse(matchesTimeControlFilter("bullet", selected))
    }

    @Test
    fun matchesTimeControlFilter_isCaseInsensitive() {
        val selected = setOf("blitz")
        assertTrue(matchesTimeControlFilter("BlItZ", selected))
    }
}


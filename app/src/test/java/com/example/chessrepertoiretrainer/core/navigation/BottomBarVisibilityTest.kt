package com.example.chessrepertoiretrainer.core.navigation

import androidx.navigation.NavDestination
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

class BottomBarVisibilityTest {

    private fun dest(route: String): NavDestination = mockk<NavDestination>().also {
        every { it.route } returns route
    }

    // ---- null destination ----

    @Test
    fun `null destination returns true`() {
        assertTrue(shouldShowBottomBar(null))
    }

    // ---- visible bottom-bar screens ----

    @Test
    fun `home route is visible`() {
        assertTrue(shouldShowBottomBar(dest("home")))
    }

    @Test
    fun `repertoire_main route is visible`() {
        assertTrue(shouldShowBottomBar(dest("repertoire_main")))
    }

    @Test
    fun `settings route is visible`() {
        assertTrue(shouldShowBottomBar(dest("settings")))
    }

    @Test
    fun `my_games route is visible`() {
        assertTrue(shouldShowBottomBar(dest("my_games")))
    }

    // ---- exact hidden routes ----

    @Test
    fun `analysis route is hidden`() {
        assertFalse(shouldShowBottomBar(dest("analysis")))
    }

    @Test
    fun `multi_chapter_training route is hidden`() {
        assertFalse(shouldShowBottomBar(dest("multi_chapter_training")))
    }

    @Test
    fun `puzzle_training route is hidden`() {
        assertFalse(shouldShowBottomBar(dest("puzzle_training")))
    }

    @Test
    fun `games_list route is hidden`() {
        assertFalse(shouldShowBottomBar(dest("games_list")))
    }

    @Test
    fun `opening_explorer route is hidden`() {
        assertFalse(shouldShowBottomBar(dest("opening_explorer")))
    }

    @Test
    fun `repertoire_puzzles route is hidden`() {
        assertFalse(shouldShowBottomBar(dest("repertoire_puzzles")))
    }

    // ---- prefix-based hidden routes ----

    @Test
    fun `line_editor with id is hidden`() {
        assertFalse(shouldShowBottomBar(dest("line_editor/42")))
    }

    @Test
    fun `chapter_learn with id is hidden`() {
        assertFalse(shouldShowBottomBar(dest("chapter_learn/5")))
    }

    @Test
    fun `game_detail with id is hidden`() {
        assertFalse(shouldShowBottomBar(dest("game_detail/abcXYZ")))
    }

    @Test
    fun `opening_tree route is hidden`() {
        assertFalse(shouldShowBottomBar(dest("opening_tree")))
    }

    @Test
    fun `account_stats route is hidden`() {
        assertFalse(shouldShowBottomBar(dest("account_stats/lichess/magnus")))
    }
}

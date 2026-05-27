package com.example.chessrepertoiretrainer.feature.openingtree.data

import org.junit.Assert.*
import org.junit.Test

class OpeningTreeBuilderTest {

    private fun game(pgn: String, isUserWhite: Boolean = true, result: String = "1-0") =
        GameForOpeningTree(pgn = pgn, isUserWhite = isUserWhite, resultTag = result)

    // ---- empty input ----

    @Test
    fun `empty game list returns null`() {
        assertNull(OpeningTreeBuilder.buildTree(emptyList(), playerIsBlack = false))
    }

    // ---- basic tree structure ----

    @Test
    fun `single game creates root and first move node`() {
        val tree = OpeningTreeBuilder.buildTree(listOf(game("1. e4 e5 1-0")), playerIsBlack = false)
        assertNotNull(tree)
        val root = tree!!.getNode(tree.rootFen)
        assertNotNull(root)
        assertTrue(root!!.children.containsKey("e4"))
    }

    @Test
    fun `child aggregate has games count 1 for single game`() {
        val tree = OpeningTreeBuilder.buildTree(listOf(game("1. e4 e5 1-0")), playerIsBlack = false)
        val root = tree!!.getNode(tree.rootFen)!!
        assertEquals(1L, root.children["e4"]!!.games)
    }

    @Test
    fun `two games with same first move aggregates to games count 2`() {
        val g1 = game("1. e4 e5 1-0")
        val g2 = game("1. e4 d5 1-0")
        val tree = OpeningTreeBuilder.buildTree(listOf(g1, g2), playerIsBlack = false)
        val root = tree!!.getNode(tree.rootFen)!!
        assertEquals(2L, root.children["e4"]!!.games)
    }

    // ---- outcome counting (tests outcomeFromResult indirectly) ----

    @Test
    fun `win result counts as win for white player`() {
        val tree = OpeningTreeBuilder.buildTree(listOf(game("1. e4 1-0", isUserWhite = true, result = "1-0")), playerIsBlack = false)
        val root = tree!!.getNode(tree.rootFen)!!
        assertEquals(1L, root.children["e4"]!!.wins)
        assertEquals(0L, root.children["e4"]!!.losses)
    }

    @Test
    fun `loss result counts as loss for white player`() {
        val tree = OpeningTreeBuilder.buildTree(listOf(game("1. e4 e5 0-1", isUserWhite = true, result = "0-1")), playerIsBlack = false)
        val root = tree!!.getNode(tree.rootFen)!!
        assertEquals(0L, root.children["e4"]!!.wins)
        assertEquals(1L, root.children["e4"]!!.losses)
    }

    @Test
    fun `draw result counts as draw`() {
        val tree = OpeningTreeBuilder.buildTree(listOf(game("1. e4 e5 1/2-1/2", isUserWhite = true, result = "1/2-1/2")), playerIsBlack = false)
        val root = tree!!.getNode(tree.rootFen)!!
        assertEquals(1L, root.children["e4"]!!.draws)
    }

    @Test
    fun `win for black player - 0-1 is a win`() {
        val tree = OpeningTreeBuilder.buildTree(listOf(game("1. e4 e5 0-1", isUserWhite = false, result = "0-1")), playerIsBlack = true)
        val root = tree!!.getNode(tree.rootFen)!!
        assertEquals(1L, root.children["e4"]!!.wins)
    }

    @Test
    fun `unknown result tag game is skipped`() {
        val tree = OpeningTreeBuilder.buildTree(listOf(game("1. e4 e5 *", isUserWhite = true, result = "*")), playerIsBlack = false)
        // * is not a recognised result, so game is skipped; root has no children
        val root = tree!!.getNode(tree.rootFen)!!
        assertTrue(root.children.isEmpty())
    }

    // ---- PGN with blank moves ----

    @Test
    fun `game with blank pgn is skipped without crash`() {
        val games = listOf(
            game("", result = "1-0"),
            game("1. d4 d5 1-0", result = "1-0")
        )
        val tree = OpeningTreeBuilder.buildTree(games, playerIsBlack = false)
        assertNotNull(tree)
        val root = tree!!.getNode(tree.rootFen)!!
        assertTrue(root.children.containsKey("d4"))
    }

    // ---- playerIsBlack flag ----

    @Test
    fun `playerIsBlack is stored on tree`() {
        val tree = OpeningTreeBuilder.buildTree(listOf(game("1. e4 1-0")), playerIsBlack = true)
        assertTrue(tree!!.playerIsBlack)
    }
}

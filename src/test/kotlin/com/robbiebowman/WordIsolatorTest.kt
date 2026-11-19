package com.robbiebowman

import kotlin.test.Test
import kotlin.test.assertEquals

class WordIsolatorTest {

    private fun grid(vararg rows: String): Crossword =
        rows.map { row -> row.map { if (it == '#') ' ' else it }.toTypedArray() }.toTypedArray()

    @Test
    fun `finds across and down words with row-column coordinates`() {
        val (across, down) = WordIsolator.getWords(grid("#cat", "dogs", "ape#"))
        assertEquals(
            listOf(WordWithCoordinate("cat", 0, 1), WordWithCoordinate("dogs", 1, 0), WordWithCoordinate("ape", 2, 0)),
            across,
        )
        assertEquals(
            listOf(
                WordWithCoordinate("da", 1, 0),
                WordWithCoordinate("cop", 0, 1),
                WordWithCoordinate("age", 0, 2),
                WordWithCoordinate("ts", 0, 3),
            ),
            down,
        )
    }

    @Test
    fun `reports the right position when a word repeats in a line`() {
        val (across, _) = WordIsolator.getWords(grid("ab#ab", "....."))
        assertEquals(listOf(0, 3), across.filter { it.x == 0 }.map { it.y })
    }

    @Test
    fun `ignores single letter runs`() {
        val (across, down) = WordIsolator.getWords(grid("a#b", "###", "c#d"))
        assertEquals(emptyList(), across)
        assertEquals(emptyList(), down)
    }

    @Test
    fun `handles non-square grids`() {
        val (across, down) = WordIsolator.getWords(grid("abc", "def"))
        assertEquals(listOf("abc", "def"), across.map { it.word })
        assertEquals(listOf("ad", "be", "cf"), down.map { it.word })
    }
}

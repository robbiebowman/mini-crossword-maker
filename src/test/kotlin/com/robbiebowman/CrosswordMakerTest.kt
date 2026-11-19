package com.robbiebowman

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class CrosswordMakerTest {

    companion object {
        private val dictionary = Dictionary()
    }

    private fun grid(vararg rows: String): Crossword =
        rows.map { row -> row.map { if (it == '#') ' ' else it }.toTypedArray() }.toTypedArray()

    private fun allWords(puzzle: Crossword): List<String> {
        val (across, down) = WordIsolator.getWords(puzzle)
        return (across + down).map { it.word }
    }

    private fun assertValidFill(puzzle: Crossword, template: Crossword, allowedExtraWords: Set<String> = emptySet()) {
        assertEquals(template.size, puzzle.size)
        template.indices.forEach { r ->
            assertEquals(template[r].size, puzzle[r].size)
            template[r].indices.forEach { c ->
                val expected = template[r][c]
                val actual = puzzle[r][c]
                when (expected) {
                    ' ' -> assertEquals(' ', actual, "Black square at $r,$c was changed")
                    '.' -> assertTrue(actual in 'a'..'z', "Cell $r,$c wasn't filled: '$actual'")
                    else -> assertEquals(expected.lowercaseChar(), actual, "Pre-filled cell $r,$c was changed")
                }
            }
        }
        val words = allWords(puzzle)
        words.forEach { assertTrue(it in dictionary || it in allowedExtraWords, "'$it' isn't a dictionary word") }
        assertEquals(words.size, words.toSet().size, "Duplicate words in $words")
    }

    @Test
    fun `random layouts produce valid puzzles without duplicates`() {
        val maker = CrosswordMaker(Random(1), dictionary)
        repeat(25) {
            val puzzle = assertNotNull(maker.createCrossword())
            val template = puzzle.map { row -> row.map { if (it == ' ') ' ' else '.' }.toTypedArray() }.toTypedArray()
            assertValidFill(puzzle, template)
        }
    }

    @Test
    fun `fills an open 5x5 grid`() {
        val template = grid(".....", ".....", ".....", ".....", ".....")
        val puzzle = assertNotNull(CrosswordMaker(Random(7), dictionary).createCrossword(template))
        assertValidFill(puzzle, template)
    }

    @Test
    fun `same seed gives the same puzzle`() {
        val a = CrosswordMaker(Random(99), dictionary).createCrossword()
        val b = CrosswordMaker(Random(99), dictionary).createCrossword()
        assertTrue(a.contentDeepEquals(b))
    }

    @Test
    fun `keeps pre-filled letters, accepting either case`() {
        val template = grid("#C...", ".....", "..T..", ".....", "....#")
        val puzzle = assertNotNull(CrosswordMaker(Random(3), dictionary).createCrossword(template))
        assertValidFill(puzzle, template)
    }

    @Test
    fun `accepts complete words in the input that aren't in the dictionary`() {
        val template = grid("zxqvw", ".....", ".....", ".....", ".....")
        // Every column would have to start with one of these letters; a solution may or may not exist,
        // but we must not crash and anything returned must keep the given row.
        val puzzle = CrosswordMaker(Random(3), dictionary).createCrossword(template, 5.seconds)
        if (puzzle != null) assertValidFill(puzzle, template, allowedExtraWords = setOf("zxqvw"))
    }

    @Test
    fun `does not modify the input grid`() {
        val template = grid(".....", ".....", ".....", ".....", ".....")
        val copy = template.map { it.copyOf() }.toTypedArray()
        CrosswordMaker(Random(5), dictionary).createCrossword(template)
        assertTrue(template.contentDeepEquals(copy))
    }

    @Test
    fun `returns null quickly for an impossible grid`() {
        val start = System.nanoTime()
        val puzzle = CrosswordMaker(Random(1), dictionary).createCrossword(grid("qqqq.", ".....", ".....", ".....", "....."))
        assertNull(puzzle)
        assertTrue(System.nanoTime() - start < 5_000_000_000, "Took too long to give up")
    }

    @Test
    fun `respects the timeout`() {
        val tiny = Dictionary.of(listOf("ab", "cd", "ac", "bd", "xy"))
        val maker = CrosswordMaker(Random(1), tiny)
        val start = System.nanoTime()
        // Random layouts are all 5x5 and can't be filled from this dictionary, so it should keep
        // retrying until the timeout.
        assertNull(maker.createCrossword(timeout = 1.seconds))
        val elapsed = (System.nanoTime() - start) / 1_000_000_000.0
        assertTrue(elapsed in 0.9..3.0, "Took ${elapsed}s")
    }

    @Test
    fun `does not reuse a word`() {
        // Every 2x2 fill from these words repeats one (e.g. ab/ba reads the same across and down).
        val symmetric = Dictionary.of(listOf("ab", "ba", "aa", "bb"))
        assertNull(CrosswordMaker(Random(1), symmetric).createCrossword(grid("..", "..")))

        // With one asymmetric option available, that's the fill we must get.
        val tiny = Dictionary.of(listOf("ab", "ba", "aa", "bb", "cd", "ac", "bd"))
        val puzzle = assertNotNull(CrosswordMaker(Random(1), tiny).createCrossword(grid("..", "..")))
        assertEquals(4, allWords(puzzle).toSet().size, "Got ${allWords(puzzle)}")
    }

    @Test
    fun `rejects ragged or invalid grids`() {
        val maker = CrosswordMaker(Random(1), dictionary)
        assertFailsWith<IllegalArgumentException> { maker.createCrossword(grid(".....", "....")) }
        assertFailsWith<IllegalArgumentException> { maker.createCrossword(grid("..?..", ".....")) }
        assertFailsWith<IllegalArgumentException> { maker.createCrossword(arrayOf()) }
    }

    @Test
    fun `is safe to share between threads`() {
        val maker = CrosswordMaker(Random(1), dictionary)
        val pool = Executors.newFixedThreadPool(4)
        try {
            val results = pool.invokeAll((1..8).map { Callable { maker.createCrossword() } })
                .map { it.get(60, TimeUnit.SECONDS) }
            results.forEach { puzzle ->
                assertNotNull(puzzle)
                val words = allWords(puzzle)
                words.forEach { assertTrue(it in dictionary) }
                assertEquals(words.size, words.toSet().size)
            }
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `layout symmetries include all rotations and reflections`() {
        val layout = listOf("x.", "..").map { it.toList() }
        val shapes = CrosswordMaker.symmetriesOf(layout).distinct().map { rows -> rows.joinToString("/") { it.joinToString("") } }
        assertEquals(setOf("x./..", ".x/..", "../.x", "../x."), shapes.toSet())

        val asymmetric = listOf("xx.", "...", "...").map { it.toList() }
        assertEquals(8, CrosswordMaker.symmetriesOf(asymmetric).distinct().size)
    }

    @Test
    fun `dictionary only contains short lowercase words`() {
        assertTrue(dictionary.size > 1000)
        assertTrue("about" in dictionary)
        assertContentEquals(listOf("about"), dictionary.matching("about"))
        assertTrue(dictionary.matching("a.o.t").contains("about"))
    }
}

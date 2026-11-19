package com.robbiebowman

import java.time.Instant
import kotlin.math.ln
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Fills mini crossword grids with dictionary words.
 *
 * Grid notation: '.' is an empty cell to fill, ' ' is a black square, and a letter is a pre-filled cell.
 *
 * A single instance is safe to share between threads.
 */
class CrosswordMaker(
    private val rng: Random = Random(Instant.now().epochSecond),
    private val dictionary: Dictionary = Dictionary(),
) {

    private val shapes: List<Crossword> = getShapesFromFile("layouts.txt")

    /**
     * Fills [initialPuzzle], or a random layout from `layouts.txt` if none is given.
     *
     * Several fills are generated and the one with the most common words is returned. When a random layout
     * is used and it can't be filled quickly, other layouts are tried until [timeout].
     * Complete words already present in [initialPuzzle] are accepted even if they aren't in the dictionary,
     * but no word will appear in the finished grid twice unless it was already duplicated in the input.
     *
     * @return the filled grid (a new array; the input isn't modified), or null if no fill was found in time.
     * @throws IllegalArgumentException if [initialPuzzle] is empty, ragged, or contains unsupported characters.
     */
    fun createCrossword(initialPuzzle: Crossword? = null, timeout: Duration = DEFAULT_TIMEOUT): Crossword? {
        val start = System.nanoTime()
        val deadline = start + timeout.inWholeNanoseconds
        val comparisonDeadline = minOf(deadline, start + COMPARISON_BUDGET.inWholeNanoseconds)
        val random = synchronized(rng) { Random(rng.nextLong()) }

        // Generate a few fills and keep the one with the most familiar words. The first fill may use the
        // whole timeout; the extra ones only get a small budget, so a hard grid doesn't take 8x as long.
        var best: Crossword? = null
        var bestScore = Double.NEGATIVE_INFINITY
        repeat(FILLS_TO_COMPARE) {
            val attemptDeadline = if (best == null) deadline else comparisonDeadline
            val fill = if (initialPuzzle != null) {
                solve(initialPuzzle, random, attemptDeadline)
            } else {
                fillRandomLayout(random, attemptDeadline)
            } ?: return best
            val score = score(fill)
            if (score > bestScore) {
                best = fill
                bestScore = score
            }
        }
        return best
    }

    private fun fillRandomLayout(random: Random, deadline: Long): Crossword? {
        while (System.nanoTime() < deadline) {
            val shape = shapes.random(random)
            val attemptDeadline = minOf(deadline, System.nanoTime() + RANDOM_LAYOUT_ATTEMPT.inWholeNanoseconds)
            solve(shape, random, attemptDeadline)?.let { return it }
        }
        return null
    }

    private fun solve(puzzle: Crossword, random: Random, deadline: Long): Crossword? {
        val grid = Grid.parse(puzzle)
        val solver = Solver(dictionary, grid, random, deadline)
        return if (solver.solve()) grid.toCrossword() else null
    }

    /**
     * Higher is better: the average word familiarity, with extra weight on the least familiar word,
     * since a single obscure entry is what makes a mini feel unfair.
     */
    private fun score(puzzle: Crossword): Double {
        val (across, down) = WordIsolator.getWords(puzzle)
        val familiarity = (across + down).map { ln(dictionary.frequency(it.word).toDouble().coerceAtLeast(1.0)) }
        if (familiarity.isEmpty()) return 0.0
        return familiarity.average() + familiarity.min()
    }

    private fun getShapesFromFile(path: String): List<Crossword> {
        val resource = this::class.java.classLoader.getResource(path) ?: error("Layout file $path not found")
        val layouts = resource.readText()
            .replace("\r", "")
            .split(Regex("\\n\\s*\\n"))
            .map { block -> block.lines().map { it.trim() }.filter { it.isNotEmpty() } }
            .filter { it.isNotEmpty() }
            .map { rows -> rows.map { row -> row.map { if (it == 'x') ' ' else '.' } } }
        return layouts
            .flatMap { layout -> symmetriesOf(layout) }
            .distinct()
            .map { shape -> shape.map { it.toTypedArray() }.toTypedArray() }
    }

    companion object {
        val DEFAULT_TIMEOUT: Duration = 30.seconds
        private val RANDOM_LAYOUT_ATTEMPT: Duration = 2.seconds
        private const val FILLS_TO_COMPARE = 8
        private val COMPARISON_BUDGET: Duration = 2.seconds

        /** All 8 rotations and reflections of a layout (some may be identical). */
        internal fun <T> symmetriesOf(layout: List<List<T>>): List<List<List<T>>> {
            val rotations = generateSequence(layout) { WordIsolator.rotate90(it) }.take(4).toList()
            return rotations + rotations.map { shape -> shape.map { it.reversed() } }
        }
    }
}

/** A mutable grid of cells stored row-major, plus the word slots (runs of 2+ open cells) it contains. */
internal class Grid(val height: Int, val width: Int, val cells: CharArray) {

    val slots: List<Slot> = findSlots()

    /** For each cell index, the slots passing through it. */
    val slotsByCell: List<List<Slot>> = cells.indices.map { cell -> slots.filter { cell in it.cells } }

    fun pattern(slot: Slot): String = String(CharArray(slot.cells.size) { cells[slot.cells[it]] })

    fun isComplete(slot: Slot): Boolean = slot.cells.all { cells[it] != EMPTY }

    fun key(): String = String(cells)

    fun toCrossword(): Crossword = Array(height) { r -> Array(width) { c -> cells[r * width + c] } }

    private fun findSlots(): List<Slot> {
        val result = mutableListOf<Slot>()
        fun collect(lines: List<List<Int>>) = lines.forEach { line ->
            var run = mutableListOf<Int>()
            (line + -1).forEach { cell ->
                if (cell != -1 && cells[cell] != BLOCK) {
                    run.add(cell)
                } else {
                    if (run.size >= 2) result.add(Slot(run))
                    run = mutableListOf()
                }
            }
        }
        collect((0 until height).map { r -> (0 until width).map { c -> r * width + c } })
        collect((0 until width).map { c -> (0 until height).map { r -> r * width + c } })
        return result
    }

    companion object {
        const val EMPTY = '.'
        const val BLOCK = ' '

        fun parse(puzzle: Crossword): Grid {
            require(puzzle.isNotEmpty() && puzzle[0].isNotEmpty()) { "Puzzle must not be empty" }
            val width = puzzle[0].size
            require(puzzle.all { it.size == width }) { "Puzzle rows must all be the same length" }
            val cells = CharArray(puzzle.size * width) { i ->
                when (val c = puzzle[i / width][i % width]) {
                    EMPTY, BLOCK -> c
                    in 'a'..'z' -> c
                    in 'A'..'Z' -> c.lowercaseChar()
                    else -> throw IllegalArgumentException("Unsupported character '$c' in puzzle")
                }
            }
            return Grid(puzzle.size, width, cells)
        }
    }
}

internal class Slot(val cells: List<Int>)

/**
 * Backtracking search that always fills the slot with the fewest remaining candidates next, and
 * remembers grid states that are known to be dead ends.
 */
private class Solver(
    private val dictionary: Dictionary,
    private val grid: Grid,
    private val random: Random,
    private val deadline: Long,
) {
    private val usedWords = mutableSetOf<String>()
    private val deadEnds = HashSet<String>()

    fun solve(): Boolean {
        // Complete words in the starting grid are taken as given, but nothing new may duplicate them.
        grid.slots.filter { grid.isComplete(it) }.forEach { usedWords.add(grid.pattern(it)) }
        val solved = try {
            search()
        } catch (e: TimeoutException) {
            false
        }
        if (solved) fillUncheckedCells()
        return solved
    }

    private fun search(): Boolean {
        if (System.nanoTime() > deadline) throw TimeoutException()

        var bestSlot: Slot? = null
        var bestCandidates: List<String> = emptyList()
        for (slot in grid.slots) {
            if (grid.isComplete(slot)) continue
            val candidates = candidatesFor(slot)
            if (candidates.isEmpty()) return false
            if (bestSlot == null || candidates.size < bestCandidates.size) {
                bestSlot = slot
                bestCandidates = candidates
            }
        }
        val slot = bestSlot ?: return true

        val key = grid.key()
        if (key in deadEnds) return false

        for (word in orderByPreference(bestCandidates)) {
            val changed = place(slot, word)
            val newWords = completedWords(slot, word, changed)
            if (newWords != null) {
                usedWords.addAll(newWords)
                if (search()) return true
                usedWords.removeAll(newWords.toSet())
            }
            changed.forEach { grid.cells[it] = Grid.EMPTY }
        }

        if (deadEnds.size < MAX_DEAD_ENDS) deadEnds.add(key)
        return false
    }

    private fun candidatesFor(slot: Slot): List<String> =
        dictionary.matching(grid.pattern(slot)).filter { it !in usedWords }

    /** Writes [word] into [slot], returning the cells that were previously empty. */
    private fun place(slot: Slot, word: String): List<Int> =
        slot.cells.withIndex().filter { (_, cell) -> grid.cells[cell] == Grid.EMPTY }.map { (i, cell) ->
            grid.cells[cell] = word[i]
            cell
        }

    /**
     * Returns every word completed by placing [word] (including [word] itself), or null if any of them
     * isn't a dictionary word or would duplicate a word already in the grid.
     */
    private fun completedWords(slot: Slot, word: String, changed: List<Int>): List<String>? {
        val words = mutableListOf(word)
        changed.flatMap { grid.slotsByCell[it] }.distinct().filter { it !== slot && grid.isComplete(it) }.forEach {
            val crossing = grid.pattern(it)
            if (crossing !in dictionary || crossing in usedWords || crossing in words) return null
            words.add(crossing)
        }
        return words
    }

    /** Random order biased towards more common words (weight = sqrt(frequency)), so fills favour familiar vocabulary. */
    private fun orderByPreference(words: List<String>): List<String> =
        words.map { word ->
            val weight = dictionary.frequency(word).toDouble().coerceAtLeast(1.0).pow(0.5)
            word to random.nextDouble().pow(1.0 / weight)
        }.sortedByDescending { it.second }.map { it.first }

    /** Cells that aren't part of any word (boxed in on all sides) just get a random letter. */
    private fun fillUncheckedCells() {
        grid.cells.indices
            .filter { grid.cells[it] == Grid.EMPTY }
            .forEach { grid.cells[it] = 'a' + random.nextInt(26) }
    }

    private class TimeoutException : RuntimeException(null, null, false, false)

    companion object {
        private const val MAX_DEAD_ENDS = 200_000
    }
}

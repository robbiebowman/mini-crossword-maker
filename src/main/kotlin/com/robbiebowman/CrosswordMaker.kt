package com.robbiebowman

import java.time.Instant
import kotlin.random.Random


class CrosswordMaker(private val rng: Random = Random(Instant.now().epochSecond)) {

    private val dictionary: Set<String>
    private val shapes: Set<Array<Array<Char?>>>

    init {
        dictionary = getBNCFWordsFromFile("1_1_all_fullalpha.txt")

        shapes = getShapesFromFile("layouts.txt")
    }

    private val dictionaryLookUps = mutableMapOf<String, Boolean>()

    private fun initialisePuzzle(): Crossword {
        return shapes.random(rng)
    }

    fun createCrossword(initialPuzzle: Crossword = initialisePuzzle()): Crossword? {
        return dfs(initialPuzzle, setOf(), 0)
    }

    private fun getContinuations(current: Crossword, iteratingOnRow: Int): List<Crossword> {
        return dictionary.mapNotNull { w ->
            val row = current[iteratingOnRow]
                .joinToString("") { it?.toString() ?: "." }
            val template = row.trim()
                .split(' ').filterNot { it.isBlank() }
                .firstOrNull { it.contains(".") }
            if (template == null) return@mapNotNull current
            if (w.length != template.length) return@mapNotNull null
            if (!template.toRegex().matches(w)) return@mapNotNull null
            val templateIndex = row.indexOf(template)
            val new = current.copyOf()
            var i = 0
            new[iteratingOnRow] = current[iteratingOnRow].mapIndexed { idx, c ->
                // Don't replace the row's chars unless within the template bounds
                if (c == ' ' || idx < templateIndex || idx >= templateIndex + template.length)
                    c
                else w[i++]
            }.toTypedArray()
            if (validContinuation(new)) new else null
        }
    }

    private fun existsInDictionary(regex: String) =
        dictionaryLookUps[regex] ?: dictionary.any { regex.toRegex().matches(it) }
            .also { dictionaryLookUps[regex] = it }

    private fun validContinuation(puzzle: Crossword): Boolean {
        val horizontalWords =
            puzzle.flatMap { it.joinToString("") { it?.toString() ?: "." }.trim().split(" +".toRegex()) }
        val (fullHorizontalWords, partialHorizontalWords) = horizontalWords.partition { "." !in it }
        val verticalWords = (0..puzzle.lastIndex)
            .flatMap { i ->
                puzzle.map { word -> word[i] }.joinToString("") { it?.toString() ?: "." }.trim().split(" +".toRegex())
            }
        val (fullVerticalWords, partialVerticalWords) = verticalWords.partition { "." !in it }
        val allFullWords = fullHorizontalWords.plus(fullVerticalWords).toSet()
        val isUnique = allFullWords.size == fullVerticalWords.plus(fullHorizontalWords).size
        val doesntResultInAnyNonWords = partialHorizontalWords.plus(partialVerticalWords).all { w ->
            w.length == 1 || existsInDictionary(w)
        } && allFullWords.all { w -> w.length == 1 || existsInDictionary(w) }
        return isUnique && doesntResultInAnyNonWords
    }

    private fun dfs(node: Crossword, visited: Set<Crossword>, depth: Int): Crossword? {
        if (node.all { it.all { it != null } }) return node
        if (node !in visited) {
            val newVisited = visited.plusElement(node)
            if (node[depth].all { it != null }) {
                // If the row we're moving onto is already full, move on
                val solution = dfs(node, visited, depth + 1)
                if (solution != null) return solution
            } else {
                getContinuations(node, depth).shuffled(rng).forEach { neighbor ->
                    val newDepth = if (neighbor[depth].any { it == null }) depth else depth + 1
                    val solution = dfs(neighbor, newVisited, newDepth)
                    if (solution != null) return solution
                }
            }
        }
        return null
    }

    private fun getShapesFromFile(path: String): Set<Array<Array<Char?>>> {
        val resource = this::class.java.classLoader.getResource(path)
        val strings = resource!!.readText().split("\\n\\n".toRegex()).flatMap { s ->
            val shape = s.split("\\n".toRegex()).map { it.map { if (it == 'x') ' ' else null } }
            shape.map { (0..3).map { rotateNTimes(it, shape) } }
        }.flatten().toSet()
        val shapes = strings.map { it.map { it.toTypedArray() }.toTypedArray() }.toSet()
        return shapes
    }

    private fun getBNCFWordsFromFile(path: String): Set<String> {
        val resource = this::class.java.classLoader.getResource(path)
        val lines = resource!!.readText().split("\\n".toRegex())
        val bncRegex = Regex("^\\t([^\\t]+)+\\t[^\\t]+\\t([^\\t]+)+\\t\\d+\\t\\d+\\t([\\d.]+)")
        val strings = lines.mapNotNull {
            val captured = bncRegex.replace(it, "$1 $2 $3").split(" ")
            if (captured.size == 1) {
                return@mapNotNull null
            }
            val (word, derived, freq) = captured
            val actualWord = if (word == "@") derived else word
            if (freq.toDouble() > 0.1) actualWord else null
        }.toSet()
        return strings
    }

    private fun getWordsFromFile(path: String): Set<String> {
        val resource = this::class.java.classLoader.getResource(path)
        val strings = resource!!.readText().split("\\n".toRegex()).filter { w ->
            w.length <= 5 && w.all { it in 'a'..'z' }
        }.toSet()
        return strings
    }

    private fun getRankedWordsFromFile(path: String, minRank: Int): Set<String> {
        val resource = this::class.java.classLoader.getResource(path)
        val strings = resource!!.readText().split("\\n".toRegex()).mapNotNull {
            val (word, rank) = it.split(" - ")
            if (rank.toInt() < minRank) null else word
        }.filter { w ->
            w.length <= 5 && w.all { it in 'a'..'z' }
        }.toSet()
        return strings
    }

    private fun <T> rotate90(matrix: List<List<T>>): List<List<T>> {
        val rowCount = matrix.size
        val colCount = matrix[0].size
        val rotated = MutableList(colCount) { MutableList(rowCount) { matrix[0][0] } }

        for (i in 0 until rowCount) {
            for (j in 0 until colCount) {
                rotated[j][rowCount - 1 - i] = matrix[i][j]
            }
        }
        return rotated
    }

    private fun <T> rotateNTimes(n: Int, matrix: List<List<T>>): List<List<T>> {
        var mutated = matrix
        for (i in 0..n) {
            mutated = rotate90(mutated)
        }
        return mutated
    }

}

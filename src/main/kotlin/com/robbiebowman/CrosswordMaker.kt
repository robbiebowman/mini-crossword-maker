package com.robbiebowman

import org.example.com.robbiebowman.generateWildcardDictionary
import java.time.Duration
import java.time.Instant
import kotlin.random.Random


class CrosswordMaker(private val rng: Random = Random(Instant.now().epochSecond)) {

    private val dictionary: Map<String, Set<String>>
    private val shapes: Set<Array<Array<Char>>>
    private val start = Instant.now()

    init {
        val words = getBNCFWordsFromFile("1_1_all_fullalpha.txt")
        dictionary = generateWildcardDictionary(words.toSet())

        shapes = getShapesFromFile("layouts.txt")
    }

    private fun initialisePuzzle(): Crossword {
        return shapes.random(rng)
    }

    fun createCrossword(initialPuzzle: Crossword = initialisePuzzle()): Crossword? {
        return dfs(initialPuzzle, setOf(), 0)
    }

    private fun getContinuations(current: Crossword, iteratingOnRow: Int): List<Crossword> {
        val row = current[iteratingOnRow]
        if (!row.contains('.')) return listOf(current)
        val (startIndex, endIndex) = getWordBoundaries(row, row.indexOf('.'), setOf(' '))
        val valid = getContinuationIntersections(current, iteratingOnRow, startIndex, endIndex)
        return valid.map { w ->
            val new = current.copyOf()
            new[iteratingOnRow] = current[iteratingOnRow].mapIndexed { idx, c ->
                if (idx < startIndex || idx > endIndex) c else w[idx - startIndex]
            }.toTypedArray()
            new
        }
    }

    private fun existsInDictionary(regex: String) = dictionary.getValue(regex).isNotEmpty()

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


    // t o #
    // a r e
    // . . .

    private fun getContinuationIntersections(
        puzzle: Crossword,
        rowIndex: Int,
        startIndex: Int,
        endIndex: Int
    ): Set<String> {
        val template =
            puzzle[rowIndex]
                .sliceArray(startIndex..endIndex)
                .joinToString("")
        val templateAnswers = dictionary.getValue(template)
        val verticalTemplates = (startIndex..endIndex).map { horizontalIndex ->
            val (verticalStart, verticalEnd) = getWordBoundaries(
                puzzle.map { it[horizontalIndex] }.toTypedArray(),
                rowIndex,
                setOf(' ')
            )
            val verticalTemplate = (verticalStart..verticalEnd).map { puzzle[it][horizontalIndex] }.joinToString("")
            val horizontalIntersection = rowIndex - verticalStart
            verticalTemplate to horizontalIntersection
        }
        val validAnswers = getAllowedHorizontalWords(templateAnswers, verticalTemplates)

        return validAnswers
    }

    private fun getAllowedHorizontalWords(
        candidates: Set<String>,
        verticalTemplates: List<Pair<String, Int>>
    ): Set<String> {
        return candidates.filter { word ->
            var i = 0
            verticalTemplates.all { (template, horizontalIntersection) ->
                val newTemplate = template.replaceRange(
                    horizontalIntersection,
                    horizontalIntersection + 1,
                    word[i++].toString()
                )
                dictionary.getValue(newTemplate).isNotEmpty()
            }
        }.toSet()
    }

    private fun <T> getWordBoundaries(array: Array<T>, startingIndex: Int, boundaryElements: Set<T>): Pair<Int, Int> {
        if (boundaryElements.contains(array[startingIndex])) throw Exception("Starting index is a boundary element")
        var startIndex = startingIndex
        var endIndex = startingIndex
        while (startIndex > 0 && !boundaryElements.contains(array[startIndex - 1])) {
            startIndex--
        }
        while (endIndex < array.lastIndex && !boundaryElements.contains(array[endIndex + 1])) {
            endIndex++
        }
        return Pair(startIndex, endIndex)
    }

    private fun dfs(node: Crossword, visited: Set<Crossword>, depth: Int): Crossword? {
        if (node.all { it.all { it != '.' } }) {
            return node
        }
        if (node !in visited) {
            val newVisited = visited.plusElement(node)
            if (node[depth].all { it != '.' }) {
                // If the row we're moving onto is already full, move on
                val solution = dfs(node, visited, depth + 1)
                if (solution != null) return solution
            } else {
                getContinuations(node, depth).shuffled(rng).forEach { neighbor ->
                    val newDepth = if (neighbor[depth].any { it == '.' }) depth else (depth + 1)
                    val solution = dfs(neighbor, newVisited, newDepth)
                    if (solution != null) return solution
                }
            }
        }
        return null
    }

    private fun getShapesFromFile(path: String): Set<Array<Array<Char>>> {
        val resource = this::class.java.classLoader.getResource(path)
        val strings = resource!!.readText().split("\\n\\n".toRegex()).flatMap { s ->
            val shape = s.split("\\n".toRegex()).map { it.map { if (it == 'x') ' ' else '.' } }
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
            if (freq.toDouble() > 0.75) actualWord else null
        }.map { it.lowercase().filter { it != '-' } }
            .filter { it.all { it in 'a'..'z' } }
            .filter { it.length <= 5 }
            .toSet()
            .minus("@")
            .minus(":")
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

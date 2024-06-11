package com.robbiebowman

import com.robbiebowman.WordIsolator.rotate90
import org.example.com.robbiebowman.Dictionary
import java.time.Instant
import kotlin.random.Random


class CrosswordMaker(private val rng: Random = Random(Instant.now().epochSecond)) {

    private val dictionary: Dictionary = Dictionary()
    private val shapes: List<Array<Array<Char>>>

    init {
        shapes = getShapesFromFile("layouts.txt")
    }

    private fun initialisePuzzle(): Crossword {
        return shapes.random(rng)
    }

    fun createCrossword(initialPuzzle: Crossword = initialisePuzzle()): Crossword? {
        val startingWords = dictionary.getExistingWords(initialPuzzle)
        dictionary.addWords(startingWords)
        return dfs(initialPuzzle, setOf(), 0)
    }

    private fun getContinuations(current: Crossword, iteratingOnRow: Int): Set<Crossword> {
        val row = current[iteratingOnRow]
        if (!row.contains('.')) return setOf(current)
        val (startIndex, endIndex) = getWordBoundaries(row, row.indexOf('.'), setOf(' '))
        val valid = getContinuationIntersections(current, iteratingOnRow, startIndex, endIndex)
        return valid.map { w ->
            val new = current.copyOf()
            new[iteratingOnRow] = current[iteratingOnRow].mapIndexed { idx, c ->
                if (idx < startIndex || idx > endIndex) c else w[idx - startIndex]
            }.toTypedArray()
            new
        }.toSet()
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
        val validAnswers = getAllowedHorizontalWords(templateAnswers, verticalTemplates, dictionary.getExistingWords(puzzle))

        return validAnswers
    }

    private fun getAllowedHorizontalWords(
        candidates: Set<String>,
        verticalTemplates: List<Pair<String, Int>>,
        existingWords: Set<String>
    ): Set<String> {
        return candidates.filter { word ->
            var i = 0
            verticalTemplates.all { (template, horizontalIntersection) ->
                val newTemplate = template.replaceRange(
                    horizontalIntersection,
                    horizontalIntersection + 1,
                    word[i++].toString()
                )
                val remainingVerticalOptions = dictionary.getValue(newTemplate) - existingWords
                remainingVerticalOptions.isNotEmpty() || template == newTemplate
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

    private fun getShapesFromFile(path: String): List<Array<Array<Char>>> {
        val resource = this::class.java.classLoader.getResource(path)
        val strings = resource!!.readText().split("\\n\\n".toRegex()).flatMap { s ->
            val shape = s.split("\\n".toRegex()).map { it.map { if (it == 'x') ' ' else '.' } }
            shape.map { (0..3).map { rotateNTimes(it, shape) } }
        }.flatten().toSet()
        val shapes = strings.map { it.map { it.toTypedArray() }.toTypedArray() }
        return shapes
    }

    private fun <T> rotateNTimes(n: Int, matrix: List<List<T>>): List<List<T>> {
        var mutated = matrix
        for (i in 0..n) {
            mutated = rotate90(mutated)
        }
        return mutated
    }

}

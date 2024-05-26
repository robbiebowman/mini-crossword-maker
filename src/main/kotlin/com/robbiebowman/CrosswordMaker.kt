package com.robbiebowman

import java.net.URI
import java.nio.file.*


class CrosswordMaker {

    private val dictionary: Set<String>
    private var fs: FileSystem? = null

    init {
        dictionary = getWordsFromFile("5-letter-words.txt")
            .plus(getWordsFromFile("4-letter-words.txt"))
            .plus(getRankedWordsFromFile("3-letter-words-ranked.txt", 3))
            .plus(getRankedWordsFromFile("2-letter-words-ranked.txt", 3))
            .plus(('a'..'z').map { it.toString() })
    }

    private val dictionaryLookUps = mutableMapOf<String, Boolean>()

    private fun initialisePuzzle(): Crossword {
        return arrayOf(
            arrayOf(null, null, null, null, null),
            arrayOf(null, ' ', null, null, null),
            arrayOf(null, ' ', ' ', null, null),
            arrayOf(null, ' ', null, null, null),
            arrayOf(null, null, null, null, null),
        )
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
                getContinuations(node, depth).shuffled().forEach { neighbor ->
                    val newDepth = if (neighbor[depth].any { it == null }) depth else depth + 1
                    val solution = dfs(neighbor, newVisited, newDepth)
                    if (solution != null) return solution
                }
            }
        }
        return null
    }

    private fun getWordsFromFile(path: String): Set<String> {
        val resource = this::class.java.classLoader.getResource(path)
        val strings =  resource!!.readText().split("\\r?\\n".toRegex()).filter { w ->
            w.length <= 5 && w.all { it in 'a'..'z' }
        }.toSet()
        return strings
    }

    private fun getRankedWordsFromFile(path: String, minRank: Int): Set<String> {
        val resource = this::class.java.classLoader.getResource(path)
        val strings =  resource!!.readText().split("\\r?\\n".toRegex()).mapNotNull {
            val (word, rank) = it.split(" - ")
            if (rank.toInt() < minRank) null else word
        }.filter { w ->
            w.length <= 5 && w.all { it in 'a'..'z' }
        }.toSet()
        return strings
    }

}

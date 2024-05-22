package org.example

import Trie

class CrosswordMaker(private val dictionary: Set<String>) {

    val trie: Trie = Trie()

    init {
        dictionary.filter { it.length == 5 }.forEach(trie::insert)
    }

    private fun initialisePuzzle(): Crossword {
        return arrayOf(
            arrayOf(null, null, null, null, null),
            arrayOf(null, null, null, null, null),
            arrayOf(null, null, null, null, null),
            arrayOf(null, null, null, null, null),
            arrayOf(null, null, null, null, null),
        )
    }

    fun createCrossword(initialPuzzle: Crossword = initialisePuzzle()): Crossword? {
        val solution = dfs(initialPuzzle, setOf(), 0)
        return solution
    }

    private fun Crossword.contains(str: String): Boolean {
        return str in this.map { it.filterNotNull().joinToString("") }
    }

    private fun getContinuations(current: Crossword, iteratingOnRow: Int): List<Crossword> {
        return dictionary.mapNotNull { w ->
            val new = current.copyOf()
            new[iteratingOnRow] = w.map { it }.toTypedArray()
            if (validContinuation(new)) new else null
        }
    }

    val dictionaryLookUps = mutableMapOf<String, Boolean>()

    private fun validContinuation(current: Crossword): Boolean {
        val horizontalWords = current.map { it.joinToString("") { it?.toString() ?: "." } }
        val (fullHorizontalWords, partialHorizontalWords) = horizontalWords.partition { "." !in it }
        val verticalWords = (0..current.lastIndex).map { i -> current.map { it[i] }.joinToString("") { it?.toString() ?: "." } }
        val (fullVerticalWords, partialVerticalWords) = verticalWords.partition { "." !in it }
        val allFullWords = fullHorizontalWords.plus(fullVerticalWords).toSet()
        val isUnique = allFullWords.size == fullVerticalWords.plus(fullHorizontalWords).size
        val doesntResultInAnyNonWords = partialHorizontalWords.plus(partialVerticalWords).all { w ->
            dictionaryLookUps[w] ?: dictionary.any { w.toRegex().matches(it) }.also { dictionaryLookUps[w] = it }
        } && allFullWords.all { w -> dictionary.contains(w) }
        return isUnique && doesntResultInAnyNonWords
    }

    fun dfs(node: Crossword, visited: Set<Crossword>, depth: Int): Crossword? {
        if (node.all { it.all { it != null } }) return node
        if (node !in visited) {
            val newVisited = visited.plusElement(node)
            getContinuations(node, depth).shuffled().forEach { neighbor ->
                val solution = dfs(neighbor, newVisited, depth + 1)
                if (solution != null) return solution
            }
        }
        return null
    }

}

typealias Crossword = Array<Array<Char?>>
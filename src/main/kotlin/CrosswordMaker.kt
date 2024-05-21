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
        val nextWords = dictionary.filter { word ->
            var i = 0
            !current.contains(word) && word.all { c ->
                val currentPrefix = current.mapNotNull { w -> w[i] }.joinToString("")
                val prefix = "$currentPrefix${word[i]}"
                i++
                trie.getChildWords(prefix).any { w ->
                    !current.contains(w)
                }
            }
        }
        return nextWords.map {
            val new = current.copyOf()
            new[iteratingOnRow] = it.map { it }.toTypedArray();
            new
        }
    }

    fun dfs(node: Crossword, visited: Set<Crossword>, depth: Int): Crossword? {
        if (node.all { it.all { it != null } }) return node
        if (node !in visited) {
            val newVisited = visited.plusElement(node)
            getContinuations(node, depth).shuffled().forEach { neighbor ->
                val solution = dfs(neighbor, newVisited, depth+1)
                if (solution != null) return solution
            }
        }
        return null
    }

}

typealias Crossword = Array<Array<Char?>>
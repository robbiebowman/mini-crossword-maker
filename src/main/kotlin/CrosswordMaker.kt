package org.example

import Trie

class CrosswordMaker(private val dictionary: Set<String>) {

    val trie: Trie = Trie()

    init {
        dictionary.filter { it.length == 5 }.forEach(trie::insert)
    }

    fun createCrossword(initialPuzzle: Crossword? = null): Crossword? {
        val puzzle = emptyList<String>()
        val solution = dfs(puzzle, setOf())
        return solution
    }

    private fun getContinuations(current: Crossword): List<Crossword> {
        val nextWords = dictionary.filter { word ->
            var i = 0
            word !in current && word.all { c ->
                val currentPrefix = current.map { w -> w[i] }.joinToString("")
                val prefix = "$currentPrefix${word[i]}"
                i++
                trie.getChildWords(prefix).filter { w -> w !in current }.isNotEmpty()
            }
        }
        return nextWords.map { current.plus(it) }
    }

    fun dfs(node: Crossword, visited: Set<Crossword>): Crossword? {
        if (node.size == 5) return node
        if (node !in visited) {
            val newVisited = visited.plusElement(node)
            getContinuations(node).shuffled().forEach { neighbor ->
                val solution = dfs(neighbor, newVisited)
                if (solution != null) return solution
            }
            return null
        } else {
            return null
        }
    }

}

typealias Crossword = List<String>
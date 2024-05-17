package org.example

import Trie

class CrosswordMaker(private val dictionary: Set<String>) {

    val trie: Trie = Trie()

    init {
        dictionary.filter { it.length == 5 }.forEach(trie::insert)
    }

    fun createCrossword(): List<String> {
        val startingWord = "strip"

        val puzzle = listOf("goose")
        val solution = dfs(puzzle, setOf())
        println(solution)
        return solution.first()
    }

    // a r o s e
    // t a r o t
    //
    //
    //

    private fun getContinuations(current: Crossword): List<Crossword> {
        val nextWords = dictionary.filter { word ->
            var i = 0
            word.all { c ->
                val currentPrefix = current.map { w -> w[i] }.joinToString("")
                val prefix = "$currentPrefix${word[i]}"
                i++
                trie.getChildWords(prefix).isNotEmpty()
            }
        }
        return nextWords.map { current.plus(it) }
    }

    fun dfs(node: Crossword, visited: Set<Crossword>): Set<Crossword> {
        if (node.size == 5) return setOf(node).also(::println)
        if (node !in visited) {
            val newVisited = visited.plusElement(node)
            return getContinuations(node).flatMap { neighbor ->
                dfs(neighbor, newVisited)
            }.toSet()
        } else {
            return emptySet()
        }
    }

}

typealias Crossword = List<String>
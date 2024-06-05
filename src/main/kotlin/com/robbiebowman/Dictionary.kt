package org.example.com.robbiebowman

import com.robbiebowman.Crossword
import com.robbiebowman.WordIsolator.rotate90

class Dictionary {

    private val dictionary: Map<String, Set<String>>

    private val existingWordLookUp = mutableMapOf<Array<Array<Char>>, Set<String>>()

    init {
        val words = getBNCFWordsFromFile("1_1_all_fullalpha.txt")
        dictionary = generateWildcardDictionary(words.toSet())
    }

    fun getValue(string: String) = dictionary.getValue(string)

    fun getExistingWords(puzzle: Crossword): Set<String> {
        if (existingWordLookUp.contains(puzzle)) return existingWordLookUp.getValue(puzzle)
        val getAllWords = { cs: List<Char> -> cs.joinToString("").split(' ') }
        val horizontalWords = puzzle.flatMap { getAllWords(it.toList()) }
        val columns = rotate90(puzzle.map { it.toList() })
        val verticalWords = columns.flatMap(getAllWords)
        val strings = (horizontalWords + verticalWords).filter { !it.contains('.') }.toSet()
        existingWordLookUp[puzzle] = strings
        return strings
    }

    private fun getBNCFWordsFromFile(path: String): Set<String> {
        val resource = this::class.java.classLoader.getResource(path)
        val lines = resource!!.readText().split("\\n".toRegex())
        val bncRegex = Regex("^\\t([^\\t]+)+\\t([^\\t]+)\\t([^\\t]+)+\\t\\d+\\t\\d+\\t([\\d.]+)")
        val strings = lines.mapNotNull {
            val captured = bncRegex.replace(it, "$1 $2 $3 $4").split(" ")
            if (captured.size == 1) {
                return@mapNotNull null
            }
            val (word, tag, derived, freq) = captured
            if (tag == "NoP" || tag == "Fore") return@mapNotNull null
            val actualWord = if (word == "@") derived else word
            if (freq.toDouble() > 0.6) {
                actualWord
            } else null
        }.map { w -> w.lowercase().filter { it != '-' } }
            .filter { it.all { it in 'a'..'z' } }
            .filter { it.length <= 5 }
            .toSet()
        return strings
    }

    private fun generateWildcardDictionary(words: Set<String>): Map<String, Set<String>> {
        val dict = mutableMapOf<String, Set<String>>().withDefault { emptySet() }
        words.forEach { w ->
            val variations = generateAllWildcardVariations(w)
            variations.forEach { v ->
                dict[v] = dict.getValue(v).plus(w)
            }
        }
        return dict
    }

    private fun generateCombinationsHelper(word: String, current: String, results: MutableList<String>) {
        if (current.length == word.length) {
            results.add(current)
            return
        }
        val nextIndex = current.length
        generateCombinationsHelper(word, "$current.", results)
        generateCombinationsHelper(word, current + word[nextIndex], results)
    }

    private fun generateAllWildcardVariations(word: String): Set<String> {
        val results = mutableListOf<String>()
        generateCombinationsHelper(word, "", results)
        return results.toSet()
    }
}
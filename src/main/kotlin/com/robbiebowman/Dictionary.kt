package com.robbiebowman

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader

/**
 * An immutable word list, indexed by every wildcard pattern a word can match ('.' = unknown letter),
 * e.g. "cat" is reachable via "cat", ".at", "c.t", "...", etc.
 *
 * Instances are immutable once built, so a single instance can safely be shared between threads.
 */
class Dictionary internal constructor(private val frequencies: Map<String, Int>) {

    constructor() : this(loadCocaWords(DEFAULT_WORD_LIST))

    private val patternIndex: Map<String, List<String>> = buildPatternIndex(frequencies.keys)

    val size: Int get() = frequencies.size

    operator fun contains(word: String): Boolean = word in frequencies

    /** Corpus frequency of [word], or 0 if it isn't in the dictionary. */
    fun frequency(word: String): Int = frequencies[word] ?: 0

    /** All words matching [pattern], where '.' matches any letter. */
    fun matching(pattern: String): List<String> = patternIndex[pattern] ?: emptyList()

    companion object {
        private const val DEFAULT_WORD_LIST = "BNC_COCA_lists.csv"
        private const val MIN_FREQUENCY = 40
        private const val MAX_LENGTH = 5

        /** Builds a dictionary from an explicit word list. Mostly useful for tests. */
        fun of(words: Collection<String>): Dictionary = Dictionary(words.associateWith { 1 })

        private fun loadCocaWords(path: String): Map<String, Int> {
            val resource = Dictionary::class.java.classLoader.getResource(path)
                ?: error("Word list $path not found on classpath")
            val frequencies = mutableMapOf<String, Int>()
            csvReader().readAll(resource.readText()).drop(1).forEach { row ->
                val relatedForms = row[2]
                relatedForms.trim('"').split(',').forEach { form ->
                    val parts = form.trim().split(' ')
                    if (parts.size != 2) return@forEach
                    val word = parts[0].lowercase()
                    val frequency = parts[1].trim('(', ')').toIntOrNull() ?: return@forEach
                    if (frequency < MIN_FREQUENCY || word.length > MAX_LENGTH) return@forEach
                    if (!word.all { it in 'a'..'z' }) return@forEach
                    // The same form can appear under several headwords; keep its highest count.
                    frequencies[word] = maxOf(frequencies[word] ?: 0, frequency)
                }
            }
            return frequencies
        }

        private fun buildPatternIndex(words: Collection<String>): Map<String, List<String>> {
            val index = HashMap<String, MutableList<String>>()
            words.forEach { word ->
                val chars = word.toCharArray()
                for (mask in 0 until (1 shl word.length)) {
                    for (i in chars.indices) {
                        chars[i] = if (mask and (1 shl i) != 0) '.' else word[i]
                    }
                    index.getOrPut(String(chars)) { mutableListOf() }.add(word)
                }
            }
            return index
        }
    }
}

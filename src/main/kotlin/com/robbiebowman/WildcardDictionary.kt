package org.example.com.robbiebowman

internal fun generateWildcardDictionary(words: Set<String>): Map<String, Set<String>> {
    val dict = mutableMapOf<String, Set<String>>().withDefault { emptySet() }
    words.forEach { w ->
        val variations = generateAllWildcardVariations(w)
        variations.forEach { v ->
            dict[v] = dict.getValue(v).plus(w)
        }
    }
    return dict
}


private fun generateAllWildcardVariations(word: String): Set<String> {
    val results = mutableListOf<String>()
    generateCombinationsHelper(word, "", results)
    return results.toSet()
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
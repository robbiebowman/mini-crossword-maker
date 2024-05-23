package org.example

class CrosswordMaker(private val dictionary: Set<String>) {

    private fun initialisePuzzle(): Crossword {
        return arrayOf(
            arrayOf(' ', ' ', null, null, null),
            arrayOf(' ', null, null, null, null),
            arrayOf(null, null, 't', null, null),
            arrayOf(null, null, null, null, ' '),
            arrayOf(null, null, null, ' ', ' '),
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
            val template = current[iteratingOnRow].joinToString("") { it?.toString() ?: "." }.trim()
            if (w.length != template.length) return@mapNotNull null
            if (!template.toRegex().matches(w)) return@mapNotNull null
            val new = current.copyOf()
            var i = 0
            new[iteratingOnRow] = current[iteratingOnRow].map { c ->
                if (c == ' ') c else w[i++]
            }.toTypedArray()
            if (validContinuation(new)) new else null
        }
    }

    private fun existsInDictionary(regex: String) =
        dictionaryLookUps[regex] ?: dictionary.any { regex.toRegex().matches(it) }
            .also { dictionaryLookUps[regex] = it }

    private val dictionaryLookUps = mutableMapOf<String, Boolean>()

    private fun validContinuation(puzzle: Crossword): Boolean {
        val horizontalWords = puzzle.map { it.joinToString("") { it?.toString() ?: "." }.trim() }
        val (fullHorizontalWords, partialHorizontalWords) = horizontalWords.partition { "." !in it }
        val verticalWords = (0..puzzle.lastIndex)
            .map { i -> puzzle.map { word -> word[i] }.joinToString("") { it?.toString() ?: "." }.trim() }
        val (fullVerticalWords, partialVerticalWords) = verticalWords.partition { "." !in it }
        val allFullWords = fullHorizontalWords.plus(fullVerticalWords).toSet()
        val isUnique = allFullWords.size == fullVerticalWords.plus(fullHorizontalWords).size
        val doesntResultInAnyNonWords = partialHorizontalWords.plus(partialVerticalWords).all { w ->
            existsInDictionary(w)
        } && allFullWords.all { w -> existsInDictionary(w) }
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
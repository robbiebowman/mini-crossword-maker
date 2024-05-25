package org.example

object WordIsolator {

    fun getWords(crossword: Crossword): Pair<List<WordWithCoordinate>, List<WordWithCoordinate>> {
        val across = crossword.flatMapIndexed { i, r -> getWordsInRow(r, i, null) }
        val down = (0..crossword.lastIndex).flatMap { i ->
            getWordsInRow(crossword.map { word -> word[i] }.toTypedArray(), null, i)
        }
        return across to down
    }

    private fun getWordsInRow(row: Array<Char?>, x: Int?, y: Int?): List<WordWithCoordinate> {
        val rowString = row.joinToString("")
        return rowString.split(' ').map { it.trim() }.filter { it.isNotBlank() && it.length > 1 }
            .map {
                val coordinate = rowString.indexOf(it)
                WordWithCoordinate(it, x ?: coordinate, y ?: coordinate)
            }
    }

    data class WordWithCoordinate(val word: String, val x: Int, val y: Int) {
        override fun toString(): String {
            return "$word - $x $y"
        }
    }
}
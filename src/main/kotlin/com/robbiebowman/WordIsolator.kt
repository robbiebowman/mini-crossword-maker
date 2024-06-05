package com.robbiebowman

object WordIsolator {

    fun getWords(crossword: Crossword): Pair<List<WordWithCoordinate>, List<WordWithCoordinate>> {
        val across = crossword.flatMapIndexed { i, r -> getWordsInRow(r, i, null) }
        val down = (0..crossword.lastIndex).flatMap { i ->
            getWordsInRow(crossword.map { word -> word[i] }.toTypedArray(), null, i)
        }
        return across to down
    }

    private fun getWordsInRow(row: Array<Char>, x: Int?, y: Int?): List<WordWithCoordinate> {
        val rowString = row.joinToString("")
        return rowString.split(' ').map { it.trim() }.filter { it.isNotBlank() && it.length > 1 }
            .map {
                val coordinate = rowString.indexOf(it)
                WordWithCoordinate(it, x ?: coordinate, y ?: coordinate)
            }
    }

    fun <T> rotate90(matrix: List<List<T>>): List<List<T>> {
        val rowCount = matrix.size
        val colCount = matrix[0].size
        val rotated = MutableList(colCount) { MutableList(rowCount) { matrix[0][0] } }

        for (i in 0 until rowCount) {
            for (j in 0 until colCount) {
                rotated[j][rowCount - 1 - i] = matrix[i][j]
            }
        }
        return rotated
    }
}
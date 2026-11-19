package com.robbiebowman

object WordIsolator {

    /**
     * Returns the across and down words (2+ letters) in [crossword].
     *
     * Coordinates are (x = row, y = column) of each word's first letter, for both directions.
     */
    fun getWords(crossword: Crossword): Pair<List<WordWithCoordinate>, List<WordWithCoordinate>> {
        val across = crossword.flatMapIndexed { row, cells ->
            getWordsInLine(cells.toList()).map { (column, word) -> WordWithCoordinate(word, row, column) }
        }
        val width = crossword.maxOfOrNull { it.size } ?: 0
        val down = (0 until width).flatMap { column ->
            val cells = crossword.map { it.getOrElse(column) { ' ' } }
            getWordsInLine(cells).map { (row, word) -> WordWithCoordinate(word, row, column) }
        }
        return across to down
    }

    /** Each run of 2+ non-block cells in [line], paired with the index it starts at. */
    private fun getWordsInLine(line: List<Char>): List<Pair<Int, String>> {
        val words = mutableListOf<Pair<Int, String>>()
        var start = 0
        while (start < line.size) {
            if (line[start] == ' ') {
                start++
                continue
            }
            var end = start
            while (end < line.size && line[end] != ' ') end++
            if (end - start > 1) words.add(start to line.subList(start, end).joinToString(""))
            start = end
        }
        return words
    }

    fun <T> rotate90(matrix: List<List<T>>): List<List<T>> {
        val rowCount = matrix.size
        val colCount = matrix[0].size
        val rotated = MutableList(colCount) { MutableList(rowCount) { matrix[0][0] } }

        for (i in 0 until rowCount) {
            for (j in 0 until colCount) {
                rotated[colCount - 1 - j][i] = matrix[i][j]
            }
        }
        return rotated
    }
}

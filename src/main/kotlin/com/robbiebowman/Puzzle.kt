package com.robbiebowman

data class Puzzle(
    val crossword: Crossword,
    val acrossWords: List<WordWithCoordinate>,
    val downWords: List<WordWithCoordinate>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Puzzle

        if (!crossword.contentDeepEquals(other.crossword)) return false
        if (acrossWords != other.acrossWords) return false

        return true
    }

    override fun hashCode(): Int {
        var result = crossword.contentDeepHashCode()
        result = 31 * result + acrossWords.hashCode()
        return result
    }
}

data class WordWithCoordinate(val word: String, val x: Int, val y: Int) {
    override fun toString(): String {
        return "$word - $x $y"
    }
}

typealias Crossword = Array<Array<Char?>>
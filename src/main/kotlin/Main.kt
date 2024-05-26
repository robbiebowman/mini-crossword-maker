package org.example

import com.robbiebowman.CrosswordMaker
import com.robbiebowman.WordIsolator

fun main() {
    val puzzle = CrosswordMaker().createCrossword()
    if(puzzle != null) {
        puzzle.forEach {
            println(it.joinToString(" "))
        }
        val (across, down) = WordIsolator.getWords(puzzle)
        println(across)
        println(down)
    }
}
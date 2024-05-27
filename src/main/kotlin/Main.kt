package org.example

import com.robbiebowman.CrosswordMaker
import com.robbiebowman.WordIsolator
import kotlin.random.Random

fun main() {
    val crosswordMaker = CrosswordMaker()
    var puzzle = crosswordMaker.createCrossword()
    if(puzzle != null) {
        puzzle.forEach {
            println(it.joinToString(" "))
        }
        val (across, down) = WordIsolator.getWords(puzzle)
        println(across)
        println(down)
    }
    puzzle = crosswordMaker.createCrossword()
    if(puzzle != null) {
        puzzle.forEach {
            println(it.joinToString(" "))
        }
        val (across, down) = WordIsolator.getWords(puzzle)
        println(across)
        println(down)
    }
}
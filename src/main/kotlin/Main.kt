package org.example

import java.nio.file.Files
import java.nio.file.Paths

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
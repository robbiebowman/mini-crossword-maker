package org.example

import java.nio.file.Files
import java.nio.file.Paths

fun main() {
    val uri = CrosswordMaker::class.java.getResource("/wordle-words.txt")?.toURI() ?: throw Exception("Couldn't get resource.")
    val strings = Files.readAllLines(Paths.get(uri)).filter { w ->
        w.length == 5 && w.all { it in 'a'..'z' }
    }.toSet()

    val puzzle = CrosswordMaker(strings).createCrossword()
    puzzle?.forEach {
        println(it.joinToString(" "))
    }
}
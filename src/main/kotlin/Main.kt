package org.example

import java.nio.file.Files
import java.nio.file.Paths

fun main() {
    val strings = getWordsFromFile("/5-letter-words.txt")
        .plus(getWordsFromFile("/4-letter-words.txt"))
        .plus(getWordsFromFile("/3-letter-words.txt"))
        .plus(getWordsFromFile("/2-letter-words.txt"))

    val puzzle = CrosswordMaker(strings).createCrossword()
    puzzle?.forEach {
        println(it.joinToString(" "))
    }
}

private fun getWordsFromFile(path: String): Set<String> {
    val uri = CrosswordMaker::class.java.getResource(path)?.toURI()
        ?: throw Exception("Couldn't get resource.")
    val strings = Files.readAllLines(Paths.get(uri)).filter { w ->
        w.length <= 5 && w.all { it in 'a'..'z' }
    }.toSet()
    return strings
}
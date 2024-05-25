package org.example

import java.nio.file.Files
import java.nio.file.Paths

fun main() {
    val puzzle = CrosswordMaker().createCrossword()
    puzzle?.forEach {
        println(it.joinToString(" "))
    }
}
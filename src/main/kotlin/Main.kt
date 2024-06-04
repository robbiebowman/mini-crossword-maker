package org.example

import com.robbiebowman.CrosswordMaker
import com.robbiebowman.WordIsolator
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

fun main() {
    val crosswordMaker = CrosswordMaker()
    val sdf = SimpleDateFormat("hh:mm:ss")
    println("Time is ${sdf.format(Date())}")
    repeat(1) {
        val puzzle = crosswordMaker.createCrossword()!!
        puzzle.forEach {
            println(it.joinToString(" "))
        }
        val (across, down) = WordIsolator.getWords(puzzle)
        println(across)
        println(down)
        println("Time is ${sdf.format(Date())}")
    }
}
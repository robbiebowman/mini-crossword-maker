package com.robbiebowman

import kotlin.random.Random
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

private const val USAGE = """Usage: gradlew run --args="[options]"

Options:
  --count N        Number of puzzles to generate (default 1)
  --seed N         Random seed, for reproducible output
  --timeout SECS   Give up on a puzzle after this many seconds (default 30)
  --layout GRID    Grid to fill, rows separated by '/': '.' = empty, '#' = black square,
                   letters = pre-filled. e.g. --layout "#..../...../...../...../....#"
                   Defaults to a random built-in layout.
  --help           Show this message"""

fun main(args: Array<String>) {
    val options = parseArgs(args)
    val rng = options["seed"]?.let { Random(it.toLong()) } ?: Random.Default
    val count = options["count"]?.toInt() ?: 1
    val timeout = options["timeout"]?.toDouble()?.seconds ?: CrosswordMaker.DEFAULT_TIMEOUT
    val layout = options["layout"]?.let(::parseLayout)

    val maker = CrosswordMaker(rng)
    repeat(count) { i ->
        if (i > 0) println()
        val start = System.nanoTime()
        val puzzle = maker.createCrossword(layout, timeout)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        if (puzzle == null) {
            println("No fill found within $timeout")
            return@repeat
        }
        puzzle.forEach { row -> println(row.joinToString(" ") { if (it == ' ') "#" else it.uppercase() }) }
        val (across, down) = WordIsolator.getWords(puzzle)
        println("Across: ${across.joinToString { it.word }}")
        println("Down:   ${down.joinToString { it.word }}")
        println("(${elapsedMs}ms)")
    }
}

private fun parseArgs(args: Array<String>): Map<String, String> {
    val options = mutableMapOf<String, String>()
    var i = 0
    while (i < args.size) {
        val name = args[i].removePrefix("--")
        if (name == "help" || name == args[i] || name !in setOf("count", "seed", "timeout", "layout")) {
            println(USAGE)
            exitProcess(if (name == "help") 0 else 1)
        }
        options[name] = args.getOrNull(i + 1) ?: run {
            println("Missing value for --$name\n\n$USAGE")
            exitProcess(1)
        }
        i += 2
    }
    return options
}

private fun parseLayout(layout: String): Crossword =
    layout.split('/').map { row -> row.map { if (it == '#') ' ' else it }.toTypedArray() }.toTypedArray()

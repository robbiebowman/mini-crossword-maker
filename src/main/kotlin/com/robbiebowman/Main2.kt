package com.robbiebowman

import org.example.com.robbiebowman.Dictionary
import java.io.File
import kotlin.random.Random

//     val allWords = Dictionary().getCOCAWordsFromFile("BNC_COCA_lists.csv")

// Slot class to represent each word slot in the grid
data class Slot(
    val id: Int,
    val isAcross: Boolean,
    val row: Int,
    val col: Int,
    val length: Int,
    var assignedWord: String? = null,
    val possibleWords: MutableSet<String> = mutableSetOf()
)

// Cell class to keep track of the slots associated with each cell
data class Cell(
    var letter: Char = ' ', var isBlack: Boolean = false, var acrossSlot: Slot? = null, var downSlot: Slot? = null
)

fun main() {
    // Load dictionary and build word lists by length
    val dictionary = mutableMapOf<Int, MutableSet<String>>()

    // Replace "dictionary.txt" with the path to your dictionary file
    val allWords = Dictionary().getCOCAWordsFromFile("BNC_COCA_lists.csv")
    for (word in allWords) {
        val w = word.trim().lowercase()
        if (w.length in 2..5 && w.all { it in 'a'..'z' }) {
            dictionary.computeIfAbsent(w.length) { mutableSetOf() }.add(w)
        }
    }

    val gridSize = 5
    val grid = Array(gridSize) { Array(gridSize) { Cell() } }
    val random = Random(System.currentTimeMillis())

    // Example grid with black squares ('#')
    /*
     * Grid representation:
     *  [ ][ ][#][ ][ ]
     *  [ ][ ][ ][ ][ ]
     *  [ ][ ][ ][ ][ ]
     *  [ ][ ][ ][ ][ ]
     *  [ ][ ][ ][ ][ ]
     */
    grid[0][2].isBlack = true // Black square at (0,2)

    // Function to identify slots in the grid
    fun identifySlots(): List<Slot> {
        val slots = mutableListOf<Slot>()
        var slotId = 0

        // Identify across slots
        for (row in 0 until gridSize) {
            var col = 0
            while (col < gridSize) {
                if (!grid[row][col].isBlack && (col == 0 || grid[row][col - 1].isBlack)) {
                    val startCol = col
                    var length = 0
                    while (col < gridSize && !grid[row][col].isBlack) {
                        length++
                        col++
                    }
                    if (length > 1) {
                        val slot = Slot(slotId++, true, row, startCol, length)
                        slots.add(slot)
                        // Map cells to this slot
                        for (i in 0 until length) {
                            grid[row][startCol + i].acrossSlot = slot
                        }
                    }
                } else {
                    col++
                }
            }
        }

        // Identify down slots
        for (col in 0 until gridSize) {
            var row = 0
            while (row < gridSize) {
                if (!grid[row][col].isBlack && (row == 0 || grid[row - 1][col].isBlack)) {
                    val startRow = row
                    var length = 0
                    while (row < gridSize && !grid[row][col].isBlack) {
                        length++
                        row++
                    }
                    if (length > 1) {
                        val slot = Slot(slotId++, false, startRow, col, length)
                        slots.add(slot)
                        // Map cells to this slot
                        for (i in 0 until length) {
                            grid[startRow + i][col].downSlot = slot
                        }
                    }
                } else {
                    row++
                }
            }
        }

        return slots
    }

    // Initialize slots and their possible words
    val slots = identifySlots()
    for (slot in slots) {
        slot.possibleWords.addAll(dictionary[slot.length] ?: emptySet())
    }

    // Function to get positions of a slot in the grid
    fun crossingSlotPositions(slot: Slot): List<Pair<Int, Int>> {
        val positions = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until slot.length) {
            val row = if (slot.isAcross) slot.row else slot.row + i
            val col = if (slot.isAcross) slot.col + i else slot.col
            positions.add(Pair(row, col))
        }
        return positions
    }

    // Function to check if assigning a word to a slot is consistent with current assignments
    fun isConsistent(slot: Slot, word: String): Boolean {
        for (i in 0 until slot.length) {
            val row = if (slot.isAcross) slot.row else slot.row + i
            val col = if (slot.isAcross) slot.col + i else slot.col
            val cell = grid[row][col]
            val letter = word[i]

            if (cell.letter != ' ' && cell.letter != letter) {
                return false
            }

            // Check the crossing slot's possible words
            val crossingSlot = if (slot.isAcross) cell.downSlot else cell.acrossSlot
            if (crossingSlot != null && crossingSlot.assignedWord == null) {
                val positions = crossingSlotPositions(crossingSlot)
                val indexInCrossingSlot = positions.indexOf(Pair(row, col))
                val matchingWords = crossingSlot.possibleWords.filter { it[indexInCrossingSlot] == letter }
                if (matchingWords.isEmpty()) {
                    return false
                }
            }
        }
        return true
    }

    // Function to assign a word to a slot and update the grid
    fun assignWord(slot: Slot, word: String) {
        slot.assignedWord = word
        for (i in 0 until slot.length) {
            val row = if (slot.isAcross) slot.row else slot.row + i
            val col = if (slot.isAcross) slot.col + i else slot.col
            grid[row][col].letter = word[i]
        }
    }

    // Function to unassign a word from a slot and update the grid
    fun unassignWord(slot: Slot) {
        slot.assignedWord = null
        for (i in 0 until slot.length) {
            val row = if (slot.isAcross) slot.row else slot.row + i
            val col = if (slot.isAcross) slot.col + i else slot.col
            val cell = grid[row][col]
            val otherSlot = if (slot.isAcross) cell.downSlot else cell.acrossSlot
            if (otherSlot == null || otherSlot.assignedWord == null) {
                cell.letter = ' '
            } else {
                cell.letter = otherSlot.assignedWord!![if (slot.isAcross) row - otherSlot.row else col - otherSlot.col]
            }
        }
    }

    // Function to backup possible words before forward checking
    fun backupPossibleWords(slot: Slot): Map<Slot, Set<String>> {
        val backups = mutableMapOf<Slot, Set<String>>()
        for (i in 0 until slot.length) {
            val row = if (slot.isAcross) slot.row else slot.row + i
            val col = if (slot.isAcross) slot.col + i else slot.col
            val cell = grid[row][col]
            val crossingSlot = if (slot.isAcross) cell.downSlot else cell.acrossSlot
            if (crossingSlot != null && crossingSlot.assignedWord == null && crossingSlot !in backups) {
                backups[crossingSlot] = HashSet(crossingSlot.possibleWords)
            }
        }
        return backups
    }

    // Function to perform forward checking after assigning a word
    fun forwardCheck(slot: Slot): Boolean {
        for (i in 0 until slot.length) {
            val row = if (slot.isAcross) slot.row else slot.row + i
            val col = if (slot.isAcross) slot.col + i else slot.col
            val cell = grid[row][col]
            val letter = cell.letter

            val crossingSlot = if (slot.isAcross) cell.downSlot else cell.acrossSlot
            if (crossingSlot != null && crossingSlot.assignedWord == null) {
                val indexInCrossingSlot = if (slot.isAcross) row - crossingSlot.row else col - crossingSlot.col
                val newPossibleWords = crossingSlot.possibleWords.filter { it[indexInCrossingSlot] == letter }
                if (newPossibleWords.isEmpty()) {
                    return false
                } else {
                    crossingSlot.possibleWords.retainAll(newPossibleWords)
                }
            }
        }
        return true
    }

    // Function to restore possible words after backtracking
    fun restorePossibleWords(backup: Map<Slot, Set<String>>) {
        for ((slot, words) in backup) {
            slot.possibleWords.clear()
            slot.possibleWords.addAll(words)
        }
    }

    // Recursive function to fill the grid using MCV and forward checking
    fun fillGrid(): Boolean {
        // Select the unassigned slot with the fewest possible words (MCV)
        val unassignedSlots = slots.filter { it.assignedWord == null }
        if (unassignedSlots.isEmpty()) {
            // All slots are assigned
            return true
        }

        val slot = unassignedSlots.minByOrNull { it.possibleWords.size } ?: return false

        // Try each possible word for this slot
        val words = slot.possibleWords.shuffled(random)
        for (word in words) {
            if (isConsistent(slot, word)) {
                // Assign the word tentatively
                assignWord(slot, word)
                // Forward checking
                val backupPossibleWords = backupPossibleWords(slot)
                if (forwardCheck(slot)) {
                    // Proceed recursively
                    if (fillGrid()) {
                        return true
                    }
                }
                // Backtrack
                restorePossibleWords(backupPossibleWords)
                unassignWord(slot)
            }
        }
        // No valid word found for this slot, backtrack
        return false
    }


    // Start filling the grid
    if (fillGrid()) {
        // Output the grid
        println("Generated 5x5 Mini Crossword with Black Squares:")
        for (row in grid) {
            println(row.joinToString("") {
                when {
                    it.isBlack -> "#"
                    it.letter != ' ' -> it.letter.toString()
                    else -> "."
                }
            })
        }
    } else {
        println("Failed to generate a crossword.")
    }
}

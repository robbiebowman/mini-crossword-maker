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
    var possibleWords: MutableSet<String> = mutableSetOf(),
    val positions: List<Pair<Int, Int>> // Precomputed positions
)

// Cell class to keep track of the slots associated with each cell
data class Cell(
    var letter: Char = ' ',
    var isBlack: Boolean = false,
    var isPreFilled: Boolean = false,
    var acrossSlot: Slot? = null,
    var downSlot: Slot? = null
)

fun main() {
    // Load dictionary and build word lists by length
    val dictionary = mutableMapOf<Int, MutableSet<String>>()
    val indexedDictionary = mutableMapOf<Int, Array<MutableMap<Char, MutableSet<String>>>>()

    // Replace "dictionary.txt" with the path to your dictionary file
    val dictionaryFile = "dictionary.txt"
    val allWords = Dictionary().getCOCAWordsFromFile("BNC_COCA_lists.csv") + "honk" + "noah" + "euros"
    for (word in allWords) {
        val w = word.trim().lowercase()
        if (w.length in 2..5 && w.all { it in 'a'..'z' }) {
            dictionary.computeIfAbsent(w.length) { mutableSetOf() }.add(w)
        }
    }

    // Preprocess the dictionary to index words by their letters at each position
    for ((length, words) in dictionary) {
        val positionMaps = Array(length) { mutableMapOf<Char, MutableSet<String>>() }
        for (word in words) {
            for (i in word.indices) {
                positionMaps[i].computeIfAbsent(word[i]) { mutableSetOf() }.add(word)
            }
        }
        indexedDictionary[length] = positionMaps
    }

    val gridSize = 5
    val grid = Array(gridSize) { Array(gridSize) { Cell() } }
    val random = Random(System.currentTimeMillis())

    // Set of used words to prevent repeats
    val usedWords = mutableSetOf<String>()

    // Example grid with black squares ('#') and pre-filled letters
    /*
     * Grid representation:
     *  [ ][ ][#][ ][ ]
     *  [ ][ ][ ][ ][ ]
     *  [R][ ][ ][ ][ ]
     *  [ ][ ][ ][ ][ ]
     *  [ ][ ][ ][ ][ ]
     */

    // Black square at (0,2)
    grid[0][3].isBlack = true
    grid[0][4].isBlack = true
    grid[1][4].isBlack = true

    grid[3][0].isBlack = true
    grid[4][0].isBlack = true
    grid[4][1].isBlack = true

    // Pre-filled letter 'R' at (2,0)
//    grid[2][0].letter = 'r'
//    grid[2][0].isPreFilled = true

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
                    val positions = mutableListOf<Pair<Int, Int>>()
                    var length = 0
                    while (col < gridSize && !grid[row][col].isBlack) {
                        positions.add(Pair(row, col))
                        length++
                        col++
                    }
                    if (length > 1) {
                        val slot = Slot(slotId++, true, row, startCol, length, positions = positions)
                        slots.add(slot)
                        // Map cells to this slot
                        for ((r, c) in positions) {
                            grid[r][c].acrossSlot = slot
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
                    val positions = mutableListOf<Pair<Int, Int>>()
                    var length = 0
                    while (row < gridSize && !grid[row][col].isBlack) {
                        positions.add(Pair(row, col))
                        length++
                        row++
                    }
                    if (length > 1) {
                        val slot = Slot(slotId++, false, startRow, col, length, positions = positions)
                        slots.add(slot)
                        // Map cells to this slot
                        for ((r, c) in positions) {
                            grid[r][c].downSlot = slot
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

    // Function to get possible words for a slot based on current constraints
    fun getPossibleWords(slot: Slot): Set<String> {
        var possibleWords = slot.possibleWords.filter { it !in usedWords }.toMutableSet()
        val positionMaps = indexedDictionary[slot.length] ?: return possibleWords

        for (i in 0 until slot.length) {
            val (row, col) = slot.positions[i]
            val cell = grid[row][col]
            val letterConstraints = mutableSetOf<Char>()

            // Collect letter constraints from pre-filled letters and assigned letters
            if (cell.letter != ' ') {
                letterConstraints.add(cell.letter)
            }

            if (letterConstraints.isNotEmpty()) {
                val matchingWords = mutableSetOf<String>()
                for (char in letterConstraints) {
                    matchingWords.addAll(positionMaps[i][char] ?: emptySet())
                }
                possibleWords = possibleWords.intersect(matchingWords).toMutableSet()
                if (possibleWords.isEmpty()) {
                    break
                }
            }
        }
        return possibleWords
    }

    // Function to calculate how constraining a word assignment is
    fun calculateConstraint(slot: Slot, word: String): Int {
        var totalPossibleWords = 0
        for (i in 0 until slot.length) {
            val (row, col) = slot.positions[i]
            val cell = grid[row][col]
            val letter = word[i]

            val crossingSlot = if (slot.isAcross) cell.downSlot else cell.acrossSlot
            if (crossingSlot != null && crossingSlot.assignedWord == null) {
                val crossingIndex = if (slot.isAcross) row - crossingSlot.row else col - crossingSlot.col
                val positionMaps = indexedDictionary[crossingSlot.length] ?: continue
                val matchingWords = positionMaps[crossingIndex][letter]?.filter { it !in usedWords } ?: emptyList()
                totalPossibleWords += matchingWords.size
            }
        }
        return totalPossibleWords
    }

    // Function to check if assigning a word to a slot is consistent with current assignments
    fun isConsistent(slot: Slot, word: String): Boolean {
        // Ensure the word hasn't been used already
        if (word in usedWords) {
            return false
        }

        for (i in 0 until slot.length) {
            val (row, col) = slot.positions[i]
            val cell = grid[row][col]
            val letter = word[i]

            // Respect pre-filled letters
            if (cell.isPreFilled && cell.letter != letter) {
                return false
            }

            // Check for conflicting assignments
            if (cell.letter != ' ' && cell.letter != letter) {
                return false
            }
        }
        return true
    }

    // Function to assign a word to a slot and update the grid
    fun assignWord(slot: Slot, word: String) {
        slot.assignedWord = word
        usedWords.add(word)
        for (i in 0 until slot.length) {
            val (row, col) = slot.positions[i]
            val cell = grid[row][col]
            cell.letter = word[i]
        }
    }

    // Function to unassign a word from a slot and update the grid
    fun unassignWord(slot: Slot) {
        usedWords.remove(slot.assignedWord)
        slot.assignedWord = null
        for (i in 0 until slot.length) {
            val (row, col) = slot.positions[i]
            val cell = grid[row][col]
            val otherSlot = if (slot.isAcross) cell.downSlot else cell.acrossSlot
            if (!cell.isPreFilled) {
                if (otherSlot == null || otherSlot.assignedWord == null) {
                    cell.letter = ' '
                } else {
                    val otherIndex = otherSlot.positions.indexOf(Pair(row, col))
                    cell.letter = otherSlot.assignedWord!![otherIndex]
                }
            }
        }
    }

    // Function to backup possible words before forward checking
    fun backupPossibleWords(slot: Slot): Map<Slot, Set<String>> {
        val backups = mutableMapOf<Slot, Set<String>>()
        for (i in 0 until slot.length) {
            val (row, col) = slot.positions[i]
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
            val (row, col) = slot.positions[i]
            val cell = grid[row][col]
            val letter = cell.letter

            val crossingSlot = if (slot.isAcross) cell.downSlot else cell.acrossSlot
            if (crossingSlot != null && crossingSlot.assignedWord == null) {
                val crossingIndex = crossingSlot.positions.indexOf(Pair(row, col))
                val positionMaps = indexedDictionary[crossingSlot.length] ?: continue
                val matchingWords = positionMaps[crossingIndex][letter]?.filter { it !in usedWords } ?: emptyList()
                if (matchingWords.isEmpty()) {
                    return false
                } else {
                    crossingSlot.possibleWords.retainAll(matchingWords)
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

    // Recursive function to fill the grid using MCV and LCV heuristics
    fun fillGrid(): Boolean {
        // Select the unassigned slot with the fewest possible words (MCV)
        val unassignedSlots = slots.filter { it.assignedWord == null }
        if (unassignedSlots.isEmpty()) {
            // All slots are assigned
            return true
        }

        // Recalculate possible words for slots based on current assignments
        for (slot in unassignedSlots) {
            slot.possibleWords = getPossibleWords(slot).toMutableSet()
        }

        val slot = unassignedSlots.minByOrNull { it.possibleWords.size } ?: return false

        // For LCV, sort the words based on how constraining they are
        val words = slot.possibleWords
            .sortedBy { word -> calculateConstraint(slot, word) }

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
        println("Generated 5x5 Mini Crossword with Optimizations:")
        for (row in grid) {
            println(row.joinToString(" ") {
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
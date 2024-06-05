package org.example.com.robbiebowman

class Dictionary {
    private val dictionary: Map<String, Set<String>>
    private val existingWordLookUp = mutableMapOf<Array<Array<Char>>, Set<String>>()
}
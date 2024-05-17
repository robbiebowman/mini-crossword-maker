class Trie {
    inner class TrieNode {
        // A map of child nodes indexed by the next character in the key
        val children: HashMap<Char, TrieNode> = HashMap()

        // A flag to indicate that this node represents the end of a key
        var endOfKey: Boolean = false
    }

    // The root node of the trie
    private val root = TrieNode()

    // Inserts a key into the trie
    fun insert(key: String) {
        var current = root
        for (i in 0 until key.length) {
            val c = key[i]
            var node = current.children[c]
            if (node == null) {
                node = TrieNode()
                current.children[c] = node
            }
            current = node
        }
        current.endOfKey = true
    }

    // Returns true if the trie contains the given key, false otherwise
    fun contains(key: String): Boolean {
        var current = root
        for (i in 0 until key.length) {
            val c = key[i]
            val node = current.children[c] ?: return false
            current = node
        }
        return current.endOfKey
    }

    // Returns true if the trie contains a key that starts with the given prefix, false otherwise
    fun startsWith(prefix: String): Boolean {
        var current = root
        for (i in 0 until prefix.length) {
            val c = prefix[i]
            val node = current.children[c] ?: return false
            current = node
        }
        return true
    }


    // Returns true if the trie contains the given key, false otherwise
    fun getNodeOrNull(key: String): TrieNode? {
        var current = root
        for (c in key) {
            val node = current.children[c] ?: return null
            current = node
        }
        return current
    }

    fun getChildWords(prefix: String): Set<String> {
        val currentNode = getNodeOrNull(prefix)
        return getAllChildWords(currentNode, prefix)
    }

    private fun getAllChildWords(node: TrieNode?, currentWord: String): Set<String> {
        if (node == null) return emptySet()
        val words: MutableSet<String> = HashSet()
        if (node.endOfKey) {
            words.add(currentWord)
        }
        node.children.forEach { (ch: Char, childNode: TrieNode?) ->
            words.addAll(
                getAllChildWords(
                    childNode,
                    currentWord + ch
                )
            )
        }
        return words
    }
}
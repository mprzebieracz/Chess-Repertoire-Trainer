package com.example.chessrepertoiretrainer.feature.openingtree.data

data class OpeningTreeCacheKey(
    val username: String,
    val platform: String,
    val color: String,
    val timeControlFilter: String,
    val maxGamesForTree: Int?
)

object OpeningTreeCache {

    private const val MAX_ENTRIES = 5

    private val cache = object : LinkedHashMap<OpeningTreeCacheKey, OpeningTree>(
        MAX_ENTRIES, 0.75f, true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<OpeningTreeCacheKey, OpeningTree>?): Boolean {
            return size > MAX_ENTRIES
        }
    }

    @Synchronized
    fun put(key: OpeningTreeCacheKey, tree: OpeningTree) {
        cache[key] = tree
    }

    @Synchronized
    fun get(key: OpeningTreeCacheKey): OpeningTree? = cache[key]

    @Synchronized
    fun remove(key: OpeningTreeCacheKey) {
        cache.remove(key)
    }

    @Synchronized
    fun clearAll() {
        cache.clear()
    }

    @Synchronized
    fun getRecentKeys(): List<OpeningTreeCacheKey> {
        return cache.keys.toList().reversed()
    }
}
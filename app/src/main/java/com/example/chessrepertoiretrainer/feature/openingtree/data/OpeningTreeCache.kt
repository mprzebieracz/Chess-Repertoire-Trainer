package com.example.chessrepertoiretrainer.feature.openingtree.data

data class OpeningTreeCacheKey(
    val username: String,
    val platform: String,
    val color: String,
    val timeControlFilter: String,
    val maxGamesForTree: Int?
)

object OpeningTreeCache {

    private val cache = mutableMapOf<OpeningTreeCacheKey, OpeningTree>()

    @Synchronized
    fun put(key: OpeningTreeCacheKey, tree: OpeningTree) {
        cache[key] = tree
    }

    @Synchronized
    fun get(key: OpeningTreeCacheKey): OpeningTree? = cache[key]

    @Synchronized
    fun clearForUser(username: String, platform: String) {
        val iterator = cache.keys.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            if (key.username.equals(username, ignoreCase = true) && key.platform.equals(
                    platform,
                    ignoreCase = true
                )
            ) {
                iterator.remove()
            }
        }
    }
}
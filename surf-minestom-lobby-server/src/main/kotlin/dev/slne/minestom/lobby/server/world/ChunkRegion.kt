package dev.slne.minestom.lobby.server.world

import dev.slne.minestom.lobby.server.config.ServerConfig

private const val MAX_REQUIRED_CHUNKS = 65_536L

/** Inclusive rectangle of chunk coordinates. */
data class ChunkRegion(
    val minChunkX: Int,
    val minChunkZ: Int,
    val maxChunkX: Int,
    val maxChunkZ: Int,
) {
    init {
        require(minChunkX <= maxChunkX && minChunkZ <= maxChunkZ) {
            "Chunk region corners are not ordered: ($minChunkX, $minChunkZ) to ($maxChunkX, $maxChunkZ)"
        }
    }

    /** Number of chunks in the region. */
    val chunkCount: Long
        get() = (maxChunkX - minChunkX + 1L) * (maxChunkZ - minChunkZ + 1L)

    /** Grows the region by [chunks] in every horizontal direction. */
    fun expandedBy(chunks: Int): ChunkRegion {
        require(chunks >= 0) { "Cannot grow a chunk region by $chunks chunks" }
        if (chunks == 0) return this

        return ChunkRegion(
            minChunkX = chunkCoordinate(minChunkX - chunks.toLong()),
            minChunkZ = chunkCoordinate(minChunkZ - chunks.toLong()),
            maxChunkX = chunkCoordinate(maxChunkX + chunks.toLong()),
            maxChunkZ = chunkCoordinate(maxChunkZ + chunks.toLong()),
        )
    }

    /** Visits every chunk of the region exactly once. */
    inline fun forEach(action: (chunkX: Int, chunkZ: Int) -> Unit) {
        for (chunkX in minChunkX..maxChunkX) {
            for (chunkZ in minChunkZ..maxChunkZ) {
                action(chunkX, chunkZ)
            }
        }
    }

    override fun toString() = "($minChunkX, $minChunkZ) to ($maxChunkX, $maxChunkZ)"

    companion object {

        /** Region spanned by two corners given in any order. Both corners belong to the region. */
        fun between(fromChunkX: Int, fromChunkZ: Int, toChunkX: Int, toChunkZ: Int) = ChunkRegion(
            minChunkX = minOf(fromChunkX, toChunkX),
            minChunkZ = minOf(fromChunkZ, toChunkZ),
            maxChunkX = maxOf(fromChunkX, toChunkX),
            maxChunkZ = maxOf(fromChunkZ, toChunkZ),
        )
    }
}

fun ServerConfig.ForceLoadConfig.chunkRegion() = ChunkRegion.between(from.x, from.z, to.x, to.z)

/**
 * Chunks that have to be resident and prepared: the playable area grown by [viewDistance] plus one.
 *
 * @param viewDistance the chunk view distance of the instance, `Instance.viewDistance()`.
 */
fun ServerConfig.ForceLoadConfig.requiredChunks(viewDistance: Int): ChunkRegion {
    require(viewDistance >= 0) {
        "The chunk view distance cannot be negative, but is $viewDistance"
    }

    val chunkRegion = chunkRegion()
    val required = chunkRegion.expandedBy(viewDistance + 1)

    require(required.chunkCount <= MAX_REQUIRED_CHUNKS) {
        "The playable area $chunkRegion grows to ${required.chunkCount} chunks ($required) at " +
                "view distance $viewDistance, more than the limit of $MAX_REQUIRED_CHUNKS. " +
                "'force-load.from' and 'force-load.to' are chunk, not block, coordinates."
    }

    return required
}

private fun chunkCoordinate(coordinate: Long): Int =
    coordinate.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()

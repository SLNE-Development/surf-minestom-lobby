package dev.slne.minestom.lobby.server.world

import dev.slne.minestom.lobby.server.config.ServerConfig.ForceLoadConfig
import dev.slne.minestom.lobby.server.config.ServerConfig.ForceLoadConfig.Chunk
import net.minestom.server.coordinate.CoordConversion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ChunkRegionTest {

    @Test
    fun `a region contains both of its corners`() {
        val region = ChunkRegion.between(2, 3, 5, 7)

        assertEquals(ChunkRegion(2, 3, 5, 7), region)
        assertEquals(4L * 5L, region.chunkCount)
    }

    @Test
    fun `a single chunk region holds exactly that chunk`() {
        val region = ChunkRegion.between(-4, 9, -4, 9)

        assertEquals(1L, region.chunkCount)
        assertEquals(listOf(-4 to 9), region.chunks())
    }

    @Test
    fun `reversed corners span the same region`() {
        val ordered = ChunkRegion.between(-2, 4, 6, -8)
        val reversed = ChunkRegion.between(6, -8, -2, 4)

        assertEquals(ChunkRegion(-2, -8, 6, 4), ordered)
        assertEquals(ordered, reversed)
    }

    @Test
    fun `negative corners count their chunks the same way`() {
        val region = ChunkRegion.between(-10, -10, -8, -7)

        assertEquals(3L * 4L, region.chunkCount)
        assertEquals(region.chunkCount, region.chunks().size.toLong())
        assertTrue(-10 to -10 in region.chunks())
        assertTrue(-8 to -7 in region.chunks())
    }

    @Test
    fun `forEach visits every chunk of the region exactly once`() {
        val region = ChunkRegion(-3, 5, 1, 9)
        val visited = HashSet<Long>()

        region.forEach { chunkX, chunkZ ->
            assertTrue(
                visited.add(CoordConversion.chunkIndex(chunkX, chunkZ)),
                "chunk ($chunkX, $chunkZ) visited twice",
            )
        }

        assertEquals(region.chunkCount, visited.size.toLong())
    }

    @Test
    fun `growing a region moves both corners outwards`() {
        assertEquals(ChunkRegion(-6, -3, 8, 11), ChunkRegion(-2, 1, 4, 7).expandedBy(4))
    }

    @Test
    fun `growing a region by zero chunks returns it unchanged`() {
        val region = ChunkRegion(-2, 1, 4, 7)

        assertSame(region, region.expandedBy(0))
    }

    @Test
    fun `growing a region cannot overflow a chunk coordinate`() {
        val region = ChunkRegion(Int.MIN_VALUE, Int.MIN_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
            .expandedBy(64)

        assertEquals(ChunkRegion(Int.MIN_VALUE, Int.MIN_VALUE, Int.MAX_VALUE, Int.MAX_VALUE), region)
    }

    @Test
    fun `a region rejects unordered corners`() {
        assertThrows<IllegalArgumentException> { ChunkRegion(4, 0, 2, 0) }
        assertThrows<IllegalArgumentException> { ChunkRegion(0, 4, 0, 2) }
    }

    @Test
    fun `a region cannot be grown by a negative amount`() {
        assertThrows<IllegalArgumentException> { ChunkRegion(0, 0, 0, 0).expandedBy(-1) }
    }

    @Test
    fun `the required region grows the playable area by the view distance plus one`() {
        val forceLoad = ForceLoadConfig(from = Chunk(-4, -6), to = Chunk(4, 6))

        assertEquals(ChunkRegion(-37, -39, 37, 39), forceLoad.requiredChunks(viewDistance = 32))
        assertEquals(75L * 79L, forceLoad.requiredChunks(viewDistance = 32).chunkCount)
    }

    @Test
    fun `the required region of a single playable chunk is the full view distance square`() {
        val forceLoad = ForceLoadConfig(from = Chunk(0, 0), to = Chunk(0, 0))
        val required = forceLoad.requiredChunks(viewDistance = 32)

        assertEquals(ChunkRegion(-33, -33, 33, 33), required)
        assertEquals(67L * 67L, required.chunkCount)
    }

    @Test
    fun `the required region reads the playable area corners in any order`() {
        val ordered = ForceLoadConfig(from = Chunk(-12, 3), to = Chunk(5, -9))
        val reversed = ForceLoadConfig(from = Chunk(5, -9), to = Chunk(-12, 3))

        assertEquals(ordered.requiredChunks(8), reversed.requiredChunks(8))
        assertEquals(ChunkRegion(-21, -18, 14, 12), ordered.requiredChunks(8))
    }

    @Test
    fun `a view distance of zero still prepares the ring around the playable area`() {
        val forceLoad = ForceLoadConfig(from = Chunk(0, 0), to = Chunk(0, 0))

        assertEquals(ChunkRegion(-1, -1, 1, 1), forceLoad.requiredChunks(viewDistance = 0))
    }

    @Test
    fun `block coordinates mistaken for chunk coordinates are rejected`() {
        val typo = ForceLoadConfig(from = Chunk(-128, -128), to = Chunk(128, 128))
        assertThrows<IllegalArgumentException> { typo.requiredChunks(viewDistance = 32) }

        val intended = ForceLoadConfig(from = Chunk(-8, -8), to = Chunk(8, 8))
        assertEquals(83L * 83L, intended.requiredChunks(viewDistance = 32).chunkCount)
    }

    @Test
    fun `a negative view distance is rejected`() {
        val forceLoad = ForceLoadConfig()

        assertThrows<IllegalArgumentException> { forceLoad.requiredChunks(viewDistance = -1) }
    }

    private fun ChunkRegion.chunks(): List<Pair<Int, Int>> = buildList {
        forEach { chunkX, chunkZ -> add(chunkX to chunkZ) }
    }
}

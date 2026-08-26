@file:Suppress("UnstableApiUsage")

package dev.slne.minestom.lobby.server.world

import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.block.Block
import net.minestom.server.network.ConnectionState
import net.minestom.server.network.packet.server.CachedPacket
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Proves what the startup warmup buys: loading a chunk leaves its packet cache empty, warming fills
 * it, and a later block change drops it again.
 */
@EnvTest
class ChunkPacketWarmupTest {

    @Test
    fun `loading a chunk leaves its packet cache empty until it is warmed`(env: Env) {
        val instance = lightingInstance(env)
        val chunk = instance.loadChunk(4, -7).join()
        val cache = chunk.fullDataPacket as CachedPacket

        assertFalse(cache.isValid, "loadChunk must not have built the chunk packet already")

        assertTrue(chunk.warmFullDataPacket())
        assertTrue(cache.isValid, "the warmup has to leave a framed packet behind")
    }

    @Test
    fun `warming a warm chunk again reuses the cached packet`(env: Env) {
        val instance = lightingInstance(env)
        val chunk = instance.loadChunk(0, 0).join()
        val cache = chunk.fullDataPacket as CachedPacket

        assertTrue(chunk.warmFullDataPacket())
        val body = cache.body(ConnectionState.PLAY)

        assertTrue(chunk.warmFullDataPacket())
        assertSame(body, cache.body(ConnectionState.PLAY), "the packet was built twice")
    }

    @Test
    fun `a block change after the warmup drops the cached packet again`(env: Env) {
        val instance = lightingInstance(env)
        val chunk = instance.loadChunk(0, 0).join()
        val cache = chunk.fullDataPacket as CachedPacket

        assertTrue(chunk.warmFullDataPacket())
        assertTrue(cache.isValid)

        instance.setBlock(1, 41, 1, Block.GLOWSTONE)

        assertFalse(cache.isValid, "a block change has to invalidate the cached chunk packet")
    }

    private fun lightingInstance(env: Env): InstanceContainer =
        (env.createFlatInstance() as InstanceContainer).apply {
            setChunkSupplier(::LightingChunk)
        }
}

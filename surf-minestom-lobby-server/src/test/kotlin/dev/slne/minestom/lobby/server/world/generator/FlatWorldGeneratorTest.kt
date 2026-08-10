package dev.slne.minestom.lobby.server.world.generator

import net.minestom.server.MinecraftServer
import net.minestom.server.instance.block.Block
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EnvTest
class FlatWorldGeneratorTest {

    @Test
    fun `fills bedrock, dirt and a grass surface at the spawn height`(env: Env) {
        val instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.setGenerator(FlatWorldGenerator)

        try {
            instance.loadChunk(0, 0).join()

            assertEquals(Block.BEDROCK, instance.getBlock(0, 0, 0))
            assertEquals(Block.DIRT, instance.getBlock(0, 1, 0))
            assertEquals(Block.DIRT, instance.getBlock(0, 63, 0))
            assertEquals(Block.GRASS_BLOCK, instance.getBlock(0, 64, 0))
            assertEquals(Block.AIR, instance.getBlock(0, 65, 0))
        } finally {
            env.destroyInstance(instance)
        }
    }
}

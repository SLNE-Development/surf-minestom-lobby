package dev.slne.minestom.lobby.server.world.entity

import net.minestom.server.MinecraftServer
import net.minestom.server.world.biome.Biome
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EnvTest
class PolarPaperWorldAccessTest {

    @Test
    fun `biome names resolve against the running server's registry`(@Suppress("UNUSED_PARAMETER") env: Env) {
        val registry = MinecraftServer.getBiomeRegistry()

        assertEquals(
            registry.getId(Biome.PLAINS),
            PolarPaperWorldAccess().getBiomeId("minecraft:plains"),
        )
    }
}

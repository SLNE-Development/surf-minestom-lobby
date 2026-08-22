package dev.slne.minestom.lobby.server.world

import net.minestom.server.MinecraftServer
import net.minestom.server.color.AlphaColor
import net.minestom.server.color.Color
import net.minestom.server.world.DimensionType
import net.minestom.server.world.attribute.EnvironmentAttribute
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EnvTest
class SpaceDimensionTest {

    @Test
    fun `space dimension uses a dark starry overworld sky`() {
        val overworld = checkNotNull(MinecraftServer.getDimensionTypeRegistry().get(DimensionType.OVERWORLD))
        val dimension = buildSpaceDimensionType(overworld)
        val attributes = dimension.attributes().entries()

        assertEquals(DimensionType.Skybox.OVERWORLD, dimension.skybox())
        assertEquals(Color(0x000005), attributes[EnvironmentAttribute.SKY_COLOR]?.argument())
        assertEquals(Color(0x000005), attributes[EnvironmentAttribute.FOG_COLOR]?.argument())
        assertEquals(AlphaColor.TRANSPARENT, attributes[EnvironmentAttribute.CLOUD_COLOR]?.argument())
        assertEquals(AlphaColor.TRANSPARENT, attributes[EnvironmentAttribute.SUNRISE_SUNSET_COLOR]?.argument())
        assertEquals(1.0f, attributes[EnvironmentAttribute.STAR_BRIGHTNESS]?.argument())
        assertEquals(0.05f, attributes[EnvironmentAttribute.SKY_LIGHT_FACTOR]?.argument())
        assertEquals(0, dimension.timelines().size())
    }
}

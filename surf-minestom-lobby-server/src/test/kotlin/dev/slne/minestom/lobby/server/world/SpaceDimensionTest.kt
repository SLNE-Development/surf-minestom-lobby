package dev.slne.minestom.lobby.server.world

import dev.slne.minestom.lobby.api.extension.getOrThrow
import net.kyori.adventure.text.format.TextColor.color
import net.minestom.server.color.AlphaColor
import net.minestom.server.registry.RegistryTag
import net.minestom.server.utils.EaseFunction
import net.minestom.server.world.DimensionType
import net.minestom.server.world.attribute.EnvironmentAttribute
import net.minestom.server.world.clock.WorldClock
import net.minestom.server.world.timeline.Timeline
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

@EnvTest
class SpaceDimensionTest {

    @Test
    fun `space timeline is correctly configured`(env: Env) {
        val key = registerSpaceTimeline()
        val timeline = env.process().timeline().get(key)
        assertNotNull(timeline) { "Timeline $key should be registered" }

        assertEquals(WorldClock.OVERWORLD, timeline.clock())
        assertEquals(24000, timeline.periodTicks())
        assertEquals(emptyMap<Any, Any>(), timeline.timeMarkers())

        val tracks = timeline.tracks()
        assertEquals(
            setOf(
                EnvironmentAttribute.SKY_LIGHT_LEVEL,
                EnvironmentAttribute.SKY_COLOR,
                EnvironmentAttribute.FOG_COLOR,
                EnvironmentAttribute.SKY_LIGHT_FACTOR,
                EnvironmentAttribute.SKY_LIGHT_COLOR,
                EnvironmentAttribute.STAR_BRIGHTNESS,
                EnvironmentAttribute.SUN_ANGLE,
                EnvironmentAttribute.MOON_ANGLE,
                EnvironmentAttribute.STAR_ANGLE,
                EnvironmentAttribute.SUNRISE_SUNSET_COLOR,
                EnvironmentAttribute.CLOUD_COLOR,
            ),
            tracks.keys
        )

        assertConstantTrack(tracks, EnvironmentAttribute.SKY_LIGHT_LEVEL, 0.26666668f)

        assertConstantTrack(tracks, EnvironmentAttribute.SKY_COLOR, color(5, 7, 12))
        assertConstantTrack(tracks, EnvironmentAttribute.FOG_COLOR, color(5, 7, 12))

        assertConstantTrack(tracks, EnvironmentAttribute.SKY_LIGHT_FACTOR, 0.0f)
        assertConstantTrack(tracks, EnvironmentAttribute.SKY_LIGHT_COLOR, color(8, 10, 16))

        assertConstantTrack(tracks, EnvironmentAttribute.STAR_BRIGHTNESS, 1.0f)

        assertConstantTrack(tracks, EnvironmentAttribute.SUN_ANGLE, 45.0f)
        assertConstantTrack(tracks, EnvironmentAttribute.MOON_ANGLE, 75.0f)
        assertConstantTrack(tracks, EnvironmentAttribute.STAR_ANGLE, 0.0f)

        assertConstantTrack(tracks, EnvironmentAttribute.SUNRISE_SUNSET_COLOR, AlphaColor.TRANSPARENT)
        assertConstantTrack(tracks, EnvironmentAttribute.CLOUD_COLOR, AlphaColor.TRANSPARENT)
    }

    @Test
    fun `space dimension uses a dark starry overworld sky`(env: Env) {
        val overworld = env.process().dimensionType().getOrThrow(DimensionType.OVERWORLD)
        val dimension = buildSpaceDimensionType(overworld)
        val attributes = dimension.attributes().entries()

        assertEquals(true, dimension.hasFixedTime())
        assertEquals(false, dimension.hasSkylight())
        assertEquals(DimensionType.Skybox.OVERWORLD, dimension.skybox())
        assertEquals(RegistryTag.direct(registerSpaceTimeline()), dimension.timelines())

        assertEquals(color(5, 7, 12), attributes[EnvironmentAttribute.SKY_COLOR]?.argument())
        assertEquals(color(5, 7, 12), attributes[EnvironmentAttribute.FOG_COLOR]?.argument())

        assertEquals(10000.0f, attributes[EnvironmentAttribute.FOG_START_DISTANCE]?.argument())
        assertEquals(10000.0f, attributes[EnvironmentAttribute.FOG_END_DISTANCE]?.argument())
        assertEquals(10000.0f, attributes[EnvironmentAttribute.SKY_FOG_END_DISTANCE]?.argument())

        assertEquals(
            AlphaColor.TRANSPARENT,
            attributes[EnvironmentAttribute.CLOUD_COLOR]?.argument()
        )
        assertEquals(
            AlphaColor.TRANSPARENT,
            attributes[EnvironmentAttribute.SUNRISE_SUNSET_COLOR]?.argument()
        )

        assertEquals(1.0f, attributes[EnvironmentAttribute.STAR_BRIGHTNESS]?.argument())

        assertEquals(0.0f, attributes[EnvironmentAttribute.SKY_LIGHT_FACTOR]?.argument())
        assertEquals(color(8, 10, 16), attributes[EnvironmentAttribute.SKY_LIGHT_COLOR]?.argument())

        assertEquals(45.0f, attributes[EnvironmentAttribute.SUN_ANGLE]?.argument())
        assertEquals(75.0f, attributes[EnvironmentAttribute.MOON_ANGLE]?.argument())
        assertEquals(0.0f, attributes[EnvironmentAttribute.STAR_ANGLE]?.argument())

        assertEquals(
            color(210, 220, 255),
            attributes[EnvironmentAttribute.AMBIENT_LIGHT_COLOR]?.argument()
        )
        assertEquals(
            color(235, 242, 255),
            attributes[EnvironmentAttribute.BLOCK_LIGHT_TINT]?.argument()
        )

        assertEquals(0.12f, dimension.ambientLight())
    }

    private fun assertConstantTrack(
        tracks: Map<EnvironmentAttribute<*>, Timeline.Track<*, *>>,
        attribute: EnvironmentAttribute<*>,
        expected: Any,
    ) {
        val track = tracks[attribute]
        assertNotNull(track) { "Track for ${attribute.key()} should be present" }

        assertInstanceOf(EnvironmentAttribute.Modifier.Override::class.java, track.modifier())
        assertEquals(listOf(Timeline.Keyframe(0, expected)), track.keyframes())
        assertEquals(EaseFunction.LINEAR, track.ease())
    }
}

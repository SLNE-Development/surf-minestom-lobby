package dev.slne.minestom.lobby.server.world

import dev.slne.minestom.lobby.api.extension.getOrThrow
import net.kyori.adventure.text.format.TextColor.color
import net.minestom.server.color.AlphaColor
import net.minestom.server.particle.Particle
import net.minestom.server.registry.RegistryTag
import net.minestom.server.utils.EaseFunction
import net.minestom.server.utils.TickUtils
import net.minestom.server.world.DimensionType
import net.minestom.server.world.attribute.AmbientParticle
import net.minestom.server.world.attribute.EnvironmentAttribute
import net.minestom.server.world.clock.WorldClock
import net.minestom.server.world.timeline.Timeline
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@EnvTest
class SpaceDimensionTest {

    private fun ticks(duration: Duration) =
        TickUtils.fromDuration(duration.toJavaDuration())

    @Test
    fun `space timeline is correctly configured`(env: Env) {
        val key = registerSpaceTimeline()
        val timeline = env.process().timeline().get(key)
        assertNotNull(timeline) { "Timeline $key should be registered" }

        val spacePeriod = ticks(1.hours)

        val nebulaStart = ticks(20.minutes)
        val nebulaFadeInEnd = ticks(22.minutes)
        val nebulaPeak = ticks(25.minutes)
        val nebulaFadeOutStart = ticks(28.minutes)
        val nebulaEnd = ticks(30.minutes)

        val eclipseStart = ticks(46.minutes + 40.seconds)
        val eclipseAlignment = ticks(48.minutes + 20.seconds)
        val eclipseEndAlignment = ticks(49.minutes + 10.seconds)
        val eclipseEnd = ticks(50.minutes + 50.seconds)

        val spaceColor = color(5, 7, 12)
        val skyLightColor = color(8, 10, 16)
        val ambientLightColor = color(210, 220, 255)

        val nebulaSkyColor = color(10, 6, 20)
        val nebulaFogColor = color(8, 6, 17)
        val nebulaAmbientLightColor = color(220, 205, 255)

        val noAmbientParticles = emptyList<AmbientParticle>()

        val lightNebulaParticles = listOf(
            AmbientParticle(Particle.END_ROD, 0.0004f),
            AmbientParticle(Particle.WHITE_ASH, 0.0008f),
        )

        val denseNebulaParticles = listOf(
            AmbientParticle(Particle.END_ROD, 0.0008f),
            AmbientParticle(Particle.WHITE_ASH, 0.0015f),
        )

        assertEquals(WorldClock.OVERWORLD, timeline.clock())
        assertEquals(spacePeriod, timeline.periodTicks())
        assertEquals(emptyMap<Any, Any>(), timeline.timeMarkers())

        val tracks = timeline.tracks()

        assertEquals(
            setOf(
                EnvironmentAttribute.SKY_LIGHT_LEVEL,
                EnvironmentAttribute.SKY_COLOR,
                EnvironmentAttribute.FOG_COLOR,
                EnvironmentAttribute.AMBIENT_LIGHT_COLOR,
                EnvironmentAttribute.AMBIENT_PARTICLES,
                EnvironmentAttribute.SKY_LIGHT_FACTOR,
                EnvironmentAttribute.SKY_LIGHT_COLOR,
                EnvironmentAttribute.STAR_BRIGHTNESS,
                EnvironmentAttribute.SUN_ANGLE,
                EnvironmentAttribute.MOON_ANGLE,
                EnvironmentAttribute.STAR_ANGLE,
                EnvironmentAttribute.SUNRISE_SUNSET_COLOR,
                EnvironmentAttribute.CLOUD_COLOR,
            ),
            tracks.keys,
        )

        assertConstantTrack(
            tracks,
            EnvironmentAttribute.SKY_LIGHT_LEVEL,
            0.26666668f,
        )

        assertTrack(
            tracks,
            EnvironmentAttribute.SKY_COLOR,
            listOf(
                Timeline.Keyframe(0, spaceColor),
                Timeline.Keyframe(nebulaStart, spaceColor),
                Timeline.Keyframe(nebulaFadeInEnd, nebulaSkyColor),
                Timeline.Keyframe(nebulaFadeOutStart, nebulaSkyColor),
                Timeline.Keyframe(nebulaEnd, spaceColor),
                Timeline.Keyframe(spacePeriod, spaceColor),
            ),
            EaseFunction.IN_OUT_SINE,
        )

        assertTrack(
            tracks,
            EnvironmentAttribute.FOG_COLOR,
            listOf(
                Timeline.Keyframe(0, spaceColor),
                Timeline.Keyframe(nebulaStart, spaceColor),
                Timeline.Keyframe(nebulaFadeInEnd, nebulaFogColor),
                Timeline.Keyframe(nebulaFadeOutStart, nebulaFogColor),
                Timeline.Keyframe(nebulaEnd, spaceColor),
                Timeline.Keyframe(spacePeriod, spaceColor),
            ),
            EaseFunction.IN_OUT_SINE,
        )

        assertTrack(
            tracks,
            EnvironmentAttribute.AMBIENT_LIGHT_COLOR,
            listOf(
                Timeline.Keyframe(0, ambientLightColor),
                Timeline.Keyframe(nebulaStart, ambientLightColor),
                Timeline.Keyframe(nebulaFadeInEnd, nebulaAmbientLightColor),
                Timeline.Keyframe(nebulaFadeOutStart, nebulaAmbientLightColor),
                Timeline.Keyframe(nebulaEnd, ambientLightColor),
                Timeline.Keyframe(spacePeriod, ambientLightColor),
            ),
            EaseFunction.IN_OUT_SINE,
        )

        assertTrack(
            tracks,
            EnvironmentAttribute.AMBIENT_PARTICLES,
            listOf(
                Timeline.Keyframe(0, noAmbientParticles),
                Timeline.Keyframe(nebulaStart, noAmbientParticles),
                Timeline.Keyframe(nebulaFadeInEnd, lightNebulaParticles),
                Timeline.Keyframe(nebulaPeak, denseNebulaParticles),
                Timeline.Keyframe(nebulaFadeOutStart, lightNebulaParticles),
                Timeline.Keyframe(nebulaEnd, noAmbientParticles),
                Timeline.Keyframe(spacePeriod, noAmbientParticles),
            ),
            EaseFunction.CONSTANT,
        )

        assertConstantTrack(
            tracks,
            EnvironmentAttribute.SKY_LIGHT_FACTOR,
            0.0f,
        )
        assertConstantTrack(
            tracks,
            EnvironmentAttribute.SKY_LIGHT_COLOR,
            skyLightColor,
        )

        assertTrack(
            tracks,
            EnvironmentAttribute.STAR_BRIGHTNESS,
            listOf(
                Timeline.Keyframe(0, 0.90f),
                Timeline.Keyframe(spacePeriod / 4, 1.0f),
                Timeline.Keyframe(spacePeriod / 2, 0.92f),
                Timeline.Keyframe(spacePeriod * 3 / 4, 0.97f),
                Timeline.Keyframe(spacePeriod, 0.90f),
            ),
            EaseFunction.LINEAR,
        )

        assertConstantTrack(
            tracks,
            EnvironmentAttribute.SUN_ANGLE,
            45.0f,
        )

        assertTrack(
            tracks,
            EnvironmentAttribute.MOON_ANGLE,
            listOf(
                Timeline.Keyframe(0, 75.0f),
                Timeline.Keyframe(eclipseStart, 75.0f),
                Timeline.Keyframe(eclipseAlignment, 45.0f),
                Timeline.Keyframe(eclipseEndAlignment, 45.0f),
                Timeline.Keyframe(eclipseEnd, 75.0f),
                Timeline.Keyframe(spacePeriod, 75.0f),
            ),
            EaseFunction.LINEAR,
        )

        assertTrack(
            tracks,
            EnvironmentAttribute.STAR_ANGLE,
            listOf(
                Timeline.Keyframe(0, 0.0f),
                Timeline.Keyframe(spacePeriod, 360.0f),
            ),
            EaseFunction.LINEAR,
        )

        assertConstantTrack(
            tracks,
            EnvironmentAttribute.SUNRISE_SUNSET_COLOR,
            AlphaColor.TRANSPARENT,
        )
        assertConstantTrack(
            tracks,
            EnvironmentAttribute.CLOUD_COLOR,
            AlphaColor.TRANSPARENT,
        )
    }

    @Test
    fun `space dimension uses a dark starry overworld sky`(env: Env) {
        val overworld = env.process()
            .dimensionType()
            .getOrThrow(DimensionType.OVERWORLD)

        val dimension = buildSpaceDimensionType(overworld)
        val attributes = dimension.attributes().entries()

        assertEquals(true, dimension.hasFixedTime())
        assertEquals(false, dimension.hasSkylight())
        assertEquals(DimensionType.Skybox.OVERWORLD, dimension.skybox())
        assertEquals(
            RegistryTag.direct(registerSpaceTimeline()),
            dimension.timelines(),
        )

        assertEquals(
            color(5, 7, 12),
            attributes[EnvironmentAttribute.SKY_COLOR]?.argument(),
        )
        assertEquals(
            color(5, 7, 12),
            attributes[EnvironmentAttribute.FOG_COLOR]?.argument(),
        )

        assertEquals(
            10_000.0f,
            attributes[EnvironmentAttribute.FOG_START_DISTANCE]?.argument(),
        )
        assertEquals(
            10_000.0f,
            attributes[EnvironmentAttribute.FOG_END_DISTANCE]?.argument(),
        )
        assertEquals(
            10_000.0f,
            attributes[EnvironmentAttribute.SKY_FOG_END_DISTANCE]?.argument(),
        )

        assertEquals(
            AlphaColor.TRANSPARENT,
            attributes[EnvironmentAttribute.CLOUD_COLOR]?.argument(),
        )
        assertEquals(
            AlphaColor.TRANSPARENT,
            attributes[EnvironmentAttribute.SUNRISE_SUNSET_COLOR]?.argument(),
        )

        assertEquals(
            1.0f,
            attributes[EnvironmentAttribute.STAR_BRIGHTNESS]?.argument(),
        )

        assertEquals(
            0.0f,
            attributes[EnvironmentAttribute.SKY_LIGHT_FACTOR]?.argument(),
        )
        assertEquals(
            color(8, 10, 16),
            attributes[EnvironmentAttribute.SKY_LIGHT_COLOR]?.argument(),
        )

        assertEquals(
            45.0f,
            attributes[EnvironmentAttribute.SUN_ANGLE]?.argument(),
        )
        assertEquals(
            75.0f,
            attributes[EnvironmentAttribute.MOON_ANGLE]?.argument(),
        )
        assertEquals(
            0.0f,
            attributes[EnvironmentAttribute.STAR_ANGLE]?.argument(),
        )

        assertEquals(
            color(210, 220, 255),
            attributes[EnvironmentAttribute.AMBIENT_LIGHT_COLOR]?.argument(),
        )
        assertEquals(
            color(235, 242, 255),
            attributes[EnvironmentAttribute.BLOCK_LIGHT_TINT]?.argument(),
        )

        assertEquals(0.12f, dimension.ambientLight())
    }

    private fun assertConstantTrack(
        tracks: Map<EnvironmentAttribute<*>, Timeline.Track<*, *>>,
        attribute: EnvironmentAttribute<*>,
        expected: Any,
    ) {
        assertTrack(
            tracks,
            attribute,
            listOf(Timeline.Keyframe(0, expected)),
            EaseFunction.LINEAR,
        )
    }

    private fun assertTrack(
        tracks: Map<EnvironmentAttribute<*>, Timeline.Track<*, *>>,
        attribute: EnvironmentAttribute<*>,
        expectedKeyframes: List<Timeline.Keyframe<*>>,
        expectedEase: EaseFunction,
    ) {
        val track = tracks[attribute]
        assertNotNull(track) { "Track for ${attribute.key()} should be present" }

        assertInstanceOf(
            EnvironmentAttribute.Modifier.Override::class.java,
            track.modifier(),
        )
        assertEquals(expectedKeyframes, track.keyframes())
        assertEquals(expectedEase, track.ease())
    }
}
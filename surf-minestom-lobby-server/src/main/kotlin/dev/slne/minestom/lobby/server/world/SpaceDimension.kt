package dev.slne.minestom.lobby.server.world

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.TextColor.color
import net.minestom.server.MinecraftServer
import net.minestom.server.color.AlphaColor
import net.minestom.server.particle.Particle
import net.minestom.server.registry.RegistryKey
import net.minestom.server.registry.RegistryTag
import net.minestom.server.utils.EaseFunction
import net.minestom.server.utils.TickUtils
import net.minestom.server.world.DimensionType
import net.minestom.server.world.attribute.AmbientParticle
import net.minestom.server.world.attribute.EnvironmentAttribute
import net.minestom.server.world.clock.WorldClock
import net.minestom.server.world.timeline.Timeline
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private fun ticks(duration: Duration) = TickUtils.fromDuration(duration.toJavaDuration())

// Timeline

private val spacePeriod = ticks(1.hours)

// Nebula: 20:00 - 30:00
private val nebulaStart = ticks(20.minutes)
private val nebulaFadeInEnd = ticks(22.minutes)
private val nebulaPeak = ticks(25.minutes)
private val nebulaFadeOutStart = ticks(28.minutes)
private val nebulaEnd = ticks(30.minutes)

// Eclipse: 46:40 - 50:50
private val eclipseStart = ticks(46.minutes + 40.seconds)
private val eclipseAlignment = ticks(48.minutes + 20.seconds)
private val eclipseEndAlignment = ticks(49.minutes + 10.seconds)
private val eclipseEnd = ticks(50.minutes + 50.seconds)

// Colors

private val spaceColor = color(5, 7, 12)
private val skyLightColor = color(8, 10, 16)
private val ambientLightColor = color(210, 220, 255)
private val blockLightTint = color(235, 242, 255)

private val nebulaSkyColor = color(10, 6, 20)
private val nebulaFogColor = color(8, 6, 17)
private val nebulaAmbientLightColor = color(220, 205, 255)

// Lighting

private const val SKY_LIGHT_LEVEL = 0.26666668f
private const val SKY_LIGHT_FACTOR = 0.0f
private const val AMBIENT_LIGHT = 0.12f

// Sky

private const val FOG_DISTANCE = 10_000.0f

private const val SUN_ANGLE = 45.0f
private const val MOON_ANGLE = 75.0f
private const val ECLIPSE_MOON_ANGLE = SUN_ANGLE

private const val STAR_BRIGHTNESS = 1.0f

// Particles

private val lightNebulaParticles = listOf(
    AmbientParticle(Particle.END_ROD, 0.0004f),
    AmbientParticle(Particle.WHITE_ASH, 0.0008f),
)

private val denseNebulaParticles = listOf(
    AmbientParticle(Particle.END_ROD, 0.0008f),
    AmbientParticle(Particle.WHITE_ASH, 0.0015f),
)

private val noAmbientParticles = emptyList<AmbientParticle>()

internal fun buildSpaceDimensionType(overworld: DimensionType): DimensionType =
    DimensionType.builder(overworld)
        .fixedTime(true)
        .skylight(false)
        .skybox(DimensionType.Skybox.OVERWORLD)
        .timelines(RegistryTag.direct(registerSpaceTimeline()))

        .setAttribute(EnvironmentAttribute.SKY_COLOR, spaceColor)
        .setAttribute(EnvironmentAttribute.FOG_COLOR, spaceColor)

        .setAttribute(EnvironmentAttribute.FOG_START_DISTANCE, FOG_DISTANCE)
        .setAttribute(EnvironmentAttribute.FOG_END_DISTANCE, FOG_DISTANCE)
        .setAttribute(EnvironmentAttribute.SKY_FOG_END_DISTANCE, FOG_DISTANCE)

        .setAttribute(EnvironmentAttribute.CLOUD_COLOR, AlphaColor.TRANSPARENT)
        .setAttribute(EnvironmentAttribute.SUNRISE_SUNSET_COLOR, AlphaColor.TRANSPARENT)

        .setAttribute(EnvironmentAttribute.STAR_BRIGHTNESS, STAR_BRIGHTNESS)

        .setAttribute(EnvironmentAttribute.SKY_LIGHT_FACTOR, SKY_LIGHT_FACTOR)
        .setAttribute(EnvironmentAttribute.SKY_LIGHT_COLOR, skyLightColor)

        .setAttribute(EnvironmentAttribute.SUN_ANGLE, SUN_ANGLE)
        .setAttribute(EnvironmentAttribute.MOON_ANGLE, MOON_ANGLE)
        .setAttribute(EnvironmentAttribute.STAR_ANGLE, 0.0f)

        .setAttribute(EnvironmentAttribute.AMBIENT_LIGHT_COLOR, ambientLightColor)
        .setAttribute(EnvironmentAttribute.BLOCK_LIGHT_TINT, blockLightTint)

        .ambientLight(AMBIENT_LIGHT)
        .build()

internal fun registerSpaceTimeline(): RegistryKey<Timeline> {
    fun <T> constantTrack(
        attribute: EnvironmentAttribute<T>,
        value: T,
    ): Timeline.Track<T, T> = Timeline.Track(
        EnvironmentAttribute.Modifier.Override(attribute.valueCodec()),
        listOf(Timeline.Keyframe(0, value)),
        EaseFunction.LINEAR,
    )

    return MinecraftServer.getTimelineRegistry().register(
        Key.key("surf", "space_timeline"),
        Timeline.builder()
            .clock(WorldClock.OVERWORLD)
            .periodTicks(spacePeriod)

            // Permanent night
            .track(
                EnvironmentAttribute.SKY_LIGHT_LEVEL,
                constantTrack(
                    EnvironmentAttribute.SKY_LIGHT_LEVEL,
                    SKY_LIGHT_LEVEL,
                ),
            )
            .track(
                EnvironmentAttribute.SKY_LIGHT_FACTOR,
                constantTrack(
                    EnvironmentAttribute.SKY_LIGHT_FACTOR,
                    SKY_LIGHT_FACTOR,
                ),
            )
            .track(
                EnvironmentAttribute.SKY_LIGHT_COLOR,
                constantTrack(
                    EnvironmentAttribute.SKY_LIGHT_COLOR,
                    skyLightColor,
                ),
            )

            // Nebula sky
            .track(
                EnvironmentAttribute.SKY_COLOR,
                Timeline.Track(
                    EnvironmentAttribute.Modifier.Override(
                        EnvironmentAttribute.SKY_COLOR.valueCodec()
                    ),
                    listOf(
                        Timeline.Keyframe(0, spaceColor),

                        Timeline.Keyframe(nebulaStart, spaceColor),
                        Timeline.Keyframe(nebulaFadeInEnd, nebulaSkyColor),

                        Timeline.Keyframe(nebulaFadeOutStart, nebulaSkyColor),
                        Timeline.Keyframe(nebulaEnd, spaceColor),

                        Timeline.Keyframe(spacePeriod, spaceColor),
                    ),
                    EaseFunction.IN_OUT_SINE,
                ),
            )
            .track(
                EnvironmentAttribute.FOG_COLOR,
                Timeline.Track(
                    EnvironmentAttribute.Modifier.Override(
                        EnvironmentAttribute.FOG_COLOR.valueCodec()
                    ),
                    listOf(
                        Timeline.Keyframe(0, spaceColor),

                        Timeline.Keyframe(nebulaStart, spaceColor),
                        Timeline.Keyframe(nebulaFadeInEnd, nebulaFogColor),

                        Timeline.Keyframe(nebulaFadeOutStart, nebulaFogColor),
                        Timeline.Keyframe(nebulaEnd, spaceColor),

                        Timeline.Keyframe(spacePeriod, spaceColor),
                    ),
                    EaseFunction.IN_OUT_SINE,
                ),
            )
            .track(
                EnvironmentAttribute.AMBIENT_LIGHT_COLOR,
                Timeline.Track(
                    EnvironmentAttribute.Modifier.Override(
                        EnvironmentAttribute.AMBIENT_LIGHT_COLOR.valueCodec()
                    ),
                    listOf(
                        Timeline.Keyframe(0, ambientLightColor),

                        Timeline.Keyframe(nebulaStart, ambientLightColor),
                        Timeline.Keyframe(nebulaFadeInEnd, nebulaAmbientLightColor),

                        Timeline.Keyframe(nebulaFadeOutStart, nebulaAmbientLightColor),
                        Timeline.Keyframe(nebulaEnd, ambientLightColor),

                        Timeline.Keyframe(spacePeriod, ambientLightColor),
                    ),
                    EaseFunction.IN_OUT_SINE,
                ),
            )

            // Nebula particles
            .track(
                EnvironmentAttribute.AMBIENT_PARTICLES,
                Timeline.Track(
                    EnvironmentAttribute.Modifier.Override(
                        EnvironmentAttribute.AMBIENT_PARTICLES.valueCodec()
                    ),
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
                ),
            )

            // Slowly pulsing stars
            .track(
                EnvironmentAttribute.STAR_BRIGHTNESS,
                Timeline.Track(
                    EnvironmentAttribute.Modifier.Override(
                        EnvironmentAttribute.STAR_BRIGHTNESS.valueCodec()
                    ),
                    listOf(
                        Timeline.Keyframe(0, 0.90f),
                        Timeline.Keyframe(spacePeriod / 4, 1.0f),
                        Timeline.Keyframe(spacePeriod / 2, 0.92f),
                        Timeline.Keyframe(spacePeriod * 3 / 4, 0.97f),
                        Timeline.Keyframe(spacePeriod, 0.90f),
                    ),
                    EaseFunction.LINEAR,
                ),
            )

            // Fixed sun
            .track(
                EnvironmentAttribute.SUN_ANGLE,
                constantTrack(
                    EnvironmentAttribute.SUN_ANGLE,
                    SUN_ANGLE,
                ),
            )

            // Eclipse
            .track(
                EnvironmentAttribute.MOON_ANGLE,
                Timeline.Track(
                    EnvironmentAttribute.Modifier.Override(
                        EnvironmentAttribute.MOON_ANGLE.valueCodec()
                    ),
                    listOf(
                        Timeline.Keyframe(0, MOON_ANGLE),

                        Timeline.Keyframe(eclipseStart, MOON_ANGLE),
                        Timeline.Keyframe(eclipseAlignment, ECLIPSE_MOON_ANGLE),

                        Timeline.Keyframe(eclipseEndAlignment, ECLIPSE_MOON_ANGLE),
                        Timeline.Keyframe(eclipseEnd, MOON_ANGLE),

                        Timeline.Keyframe(spacePeriod, MOON_ANGLE),
                    ),
                    EaseFunction.LINEAR,
                ),
            )

            // Slowly rotating star field
            .track(
                EnvironmentAttribute.STAR_ANGLE,
                Timeline.Track(
                    EnvironmentAttribute.Modifier.Override(
                        EnvironmentAttribute.STAR_ANGLE.valueCodec()
                    ),
                    listOf(
                        Timeline.Keyframe(0, 0.0f),
                        Timeline.Keyframe(spacePeriod, 360.0f),
                    ),
                    EaseFunction.LINEAR,
                ),
            )

            .track(
                EnvironmentAttribute.SUNRISE_SUNSET_COLOR,
                constantTrack(
                    EnvironmentAttribute.SUNRISE_SUNSET_COLOR,
                    AlphaColor.TRANSPARENT,
                ),
            )
            .track(
                EnvironmentAttribute.CLOUD_COLOR,
                constantTrack(
                    EnvironmentAttribute.CLOUD_COLOR,
                    AlphaColor.TRANSPARENT,
                ),
            )

            .build(),
    )
}
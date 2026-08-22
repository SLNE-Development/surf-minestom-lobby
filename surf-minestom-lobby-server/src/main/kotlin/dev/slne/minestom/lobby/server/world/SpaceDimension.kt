package dev.slne.minestom.lobby.server.world

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.TextColor.color
import net.minestom.server.MinecraftServer
import net.minestom.server.color.AlphaColor
import net.minestom.server.registry.RegistryKey
import net.minestom.server.registry.RegistryTag
import net.minestom.server.utils.EaseFunction
import net.minestom.server.world.DimensionType
import net.minestom.server.world.attribute.EnvironmentAttribute
import net.minestom.server.world.clock.WorldClock
import net.minestom.server.world.timeline.Timeline

private val spaceBlack = color(5, 7, 12)

internal fun buildSpaceDimensionType(overworld: DimensionType): DimensionType = DimensionType.builder(overworld)
    .fixedTime(true)
    .skylight(false)
    .skybox(DimensionType.Skybox.OVERWORLD)
    .timelines(RegistryTag.direct(registerSpaceTimeline()))

    .setAttribute(EnvironmentAttribute.SKY_COLOR, spaceBlack)
    .setAttribute(EnvironmentAttribute.FOG_COLOR, spaceBlack)

    .setAttribute(EnvironmentAttribute.FOG_START_DISTANCE, 10000.0f)
    .setAttribute(EnvironmentAttribute.FOG_END_DISTANCE, 10000.0f)
    .setAttribute(EnvironmentAttribute.SKY_FOG_END_DISTANCE, 10000.0f)

    .setAttribute(EnvironmentAttribute.CLOUD_COLOR, AlphaColor.TRANSPARENT)
    .setAttribute(EnvironmentAttribute.SUNRISE_SUNSET_COLOR, AlphaColor.TRANSPARENT)

    .setAttribute(EnvironmentAttribute.STAR_BRIGHTNESS, 1.0f)

    .setAttribute(EnvironmentAttribute.SKY_LIGHT_FACTOR, 0.0f)
    .setAttribute(EnvironmentAttribute.SKY_LIGHT_COLOR, color(8, 10, 16))

    .setAttribute(EnvironmentAttribute.SUN_ANGLE, 45.0f)
    .setAttribute(EnvironmentAttribute.MOON_ANGLE, 75.0f)
    .setAttribute(EnvironmentAttribute.STAR_ANGLE, 0.0f)

    .setAttribute(
        EnvironmentAttribute.AMBIENT_LIGHT_COLOR,
        color(210, 220, 255)
    )
    .setAttribute(
        EnvironmentAttribute.BLOCK_LIGHT_TINT,
        color(235, 242, 255)
    )

    .ambientLight(0.12f)
    .build()

private fun registerSpaceTimeline(): RegistryKey<Timeline> {
    fun <T> constantTrack(
        attribute: EnvironmentAttribute<T>,
        value: T,
    ): Timeline.Track<T, T> = Timeline.Track(
        EnvironmentAttribute.Modifier.Override(attribute.valueCodec()),
        listOf(Timeline.Keyframe(0, value)),
        EaseFunction.LINEAR,
    )

    val timeline = MinecraftServer.getTimelineRegistry()
        .register(
            Key.key("surf", "space_timeline"), Timeline.builder()
                .clock(WorldClock.OVERWORLD)
                .periodTicks(24000)

                .track(
                    EnvironmentAttribute.SKY_LIGHT_LEVEL,
                    constantTrack(
                        EnvironmentAttribute.SKY_LIGHT_LEVEL,
                        0.26666668f,
                    ),
                )

                .track(
                    EnvironmentAttribute.SKY_COLOR,
                    constantTrack(
                        EnvironmentAttribute.SKY_COLOR,
                        spaceBlack,
                    ),
                )
                .track(
                    EnvironmentAttribute.FOG_COLOR,
                    constantTrack(
                        EnvironmentAttribute.FOG_COLOR,
                        spaceBlack,
                    ),
                )

                .track(
                    EnvironmentAttribute.SKY_LIGHT_FACTOR,
                    constantTrack(
                        EnvironmentAttribute.SKY_LIGHT_FACTOR,
                        0.0f,
                    ),
                )
                .track(
                    EnvironmentAttribute.SKY_LIGHT_COLOR,
                    constantTrack(
                        EnvironmentAttribute.SKY_LIGHT_COLOR,
                        color(8, 10, 16),
                    ),
                )

                .track(
                    EnvironmentAttribute.STAR_BRIGHTNESS,
                    constantTrack(
                        EnvironmentAttribute.STAR_BRIGHTNESS,
                        1.0f,
                    ),
                )

                .track(
                    EnvironmentAttribute.SUN_ANGLE,
                    constantTrack(
                        EnvironmentAttribute.SUN_ANGLE,
                        45.0f,
                    ),
                )
                .track(
                    EnvironmentAttribute.MOON_ANGLE,
                    constantTrack(
                        EnvironmentAttribute.MOON_ANGLE,
                        75.0f,
                    ),
                )
                .track(
                    EnvironmentAttribute.STAR_ANGLE,
                    constantTrack(
                        EnvironmentAttribute.STAR_ANGLE,
                        0.0f,
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
                .build()
        )
    return timeline
}


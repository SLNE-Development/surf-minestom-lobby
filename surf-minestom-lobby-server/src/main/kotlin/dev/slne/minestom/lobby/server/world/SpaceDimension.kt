package dev.slne.minestom.lobby.server.world

import net.minestom.server.color.AlphaColor
import net.minestom.server.color.Color
import net.minestom.server.registry.RegistryTag
import net.minestom.server.world.DimensionType
import net.minestom.server.world.attribute.EnvironmentAttribute

internal fun buildSpaceDimensionType(overworld: DimensionType): DimensionType =
    DimensionType.builder(overworld)
        .skybox(DimensionType.Skybox.OVERWORLD)
        .timelines(RegistryTag.empty())
        .setAttribute(EnvironmentAttribute.SKY_COLOR, Color(0x000005))
        .setAttribute(EnvironmentAttribute.FOG_COLOR, Color(0x000005))
        .setAttribute(EnvironmentAttribute.CLOUD_COLOR, AlphaColor.TRANSPARENT)
        .setAttribute(EnvironmentAttribute.SUNRISE_SUNSET_COLOR, AlphaColor.TRANSPARENT)
        .setAttribute(EnvironmentAttribute.STAR_BRIGHTNESS, 1.0f)
        .setAttribute(EnvironmentAttribute.SKY_LIGHT_FACTOR, 0.05f)
        .build()

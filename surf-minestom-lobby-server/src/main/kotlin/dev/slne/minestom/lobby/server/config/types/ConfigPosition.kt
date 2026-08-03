package dev.slne.minestom.lobby.server.config.types

import net.minestom.server.coordinate.Pos
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ConfigPosition(
    val x: Double = 0.5,
    val y: Double = 64.0,
    val z: Double = 0.5,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
) {
    fun toPos(): Pos = Pos(x, y, z, yaw, pitch)
}
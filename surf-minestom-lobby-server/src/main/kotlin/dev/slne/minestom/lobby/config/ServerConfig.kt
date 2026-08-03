package dev.slne.minestom.lobby.config

import dev.slne.minestom.lobby.config.contraints.NonBlank
import dev.slne.minestom.lobby.config.types.ConfigPosition
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class ServerConfig(
    @Setting("address")
    val address: AddressConfig = AddressConfig(),

    @Setting("spawn")
    val spawn: ConfigPosition = ConfigPosition(),

    @Setting("velocity")
    val velocity: VelocityConfig = VelocityConfig(),

    @Setting("_config-version")
    @Comment(
        """
        Internal configuration version.
        Do not change this manually.
        """
    )
    val configVersion: Int = CURRENT_VERSION
) {
    companion object {
        const val CURRENT_VERSION = 1
    }

    @ConfigSerializable
    data class AddressConfig(
        @Comment("Address on which the server listens.")
        @NonBlank
        val host: String = "0.0.0.0",

        @Comment("Port on which the server listens.")
        val port: Int = 25565,
    )

    @ConfigSerializable
    data class VelocityConfig(
        val enabled: Boolean = false,
        val secret: String = "secret"
    )
}
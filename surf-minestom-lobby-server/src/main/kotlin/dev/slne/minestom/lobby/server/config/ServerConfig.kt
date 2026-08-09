package dev.slne.minestom.lobby.server.config

import dev.slne.minestom.lobby.bootstrapLogger
import dev.slne.minestom.lobby.server.config.contraints.NonBlank
import dev.slne.minestom.lobby.server.config.types.ConfigPosition
import net.kyori.adventure.text.logger.slf4j.ComponentLogger.logger
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import org.spongepowered.configurate.objectmapping.meta.PostProcess
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class ServerConfig(
    @Setting("address")
    val address: AddressConfig = AddressConfig(),

    @Setting("spawn")
    val spawn: ConfigPosition = ConfigPosition(),

    @Setting("velocity")
    val velocity: VelocityConfig = VelocityConfig(),

    @Setting("performance")
    val performance: PerformanceConfig = PerformanceConfig(),

    @Setting("chat")
    val chat: ChatConfig = ChatConfig(),

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
        private const val DISPATCHER_THREADS_PROPERTY = "minestom.dispatcher-threads"
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

    @ConfigSerializable
    data class ChatConfig(
        @Setting("enforce-secure-profile")
        @Comment(
            """
            Whether clients must present a signed chat session before they may chat.
            When enabled, players without a Mojang-issued profile key (offline mode,
            some proxies, Geyser) are told chat is disabled instead of being allowed
            to send unsigned messages.
            """
        )
        val enforceSecureProfile: Boolean = false,

        @Setting("chat-spam-threshold-seconds")
        @Comment(
            """
            Vanilla's chat-spam-threshold-seconds. Each chat message adds 20 to a
            counter that drains by 1 per tick; exceeding 20 * this value disconnects
            the player with "disconnect.spam".
            """
        )
        val chatSpamThresholdSeconds: Int = 10,

        @Setting("command-spam-threshold-seconds")
        @Comment("Vanilla's command-spam-threshold-seconds. Same mechanism as above, for commands.")
        val commandSpamThresholdSeconds: Int = 10,
    )

    @ConfigSerializable
    data class PerformanceConfig(
        @Setting("tick-threads")
        @Comment(
            """
            Number of threads used to tick the world (chunks & entities).
            Set to 0 to use the number of available CPU cores.
            For large player counts a good starting point is the number of
            physical cores.
            An explicit -Dminestom.dispatcher-threads=<n> JVM flag overrides this.
            """
        )
        val tickThreads: Int = 1,
    )

    @PostProcess
    fun applyDispatcherThreads() {
        val existing = System.getProperty(DISPATCHER_THREADS_PROPERTY)
        if (existing != null) {
            bootstrapLogger.info(
                "Tick dispatcher threads pinned via -D{}={}; keeping it.",
                DISPATCHER_THREADS_PROPERTY,
                existing
            )
            return
        }

        val configured = performance.tickThreads
        val threads = if (configured <= 0) Runtime.getRuntime().availableProcessors() else configured

        System.setProperty(DISPATCHER_THREADS_PROPERTY, threads.toString())
        bootstrapLogger.info("Using {} tick dispatcher thread(s).", threads)
    }
}
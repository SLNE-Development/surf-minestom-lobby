package dev.slne.minestom.lobby.server.bootstrap

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Stage
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.config.ServerConfigLoader
import dev.slne.minestom.lobby.server.di.LobbyServerModule
import dev.slne.minestom.lobby.server.lifecycle.LobbyServerApplication
import dev.slne.minestom.lobby.server.performance.EntityTickFilter
import kotlinx.coroutines.runBlocking
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.EntityType
import kotlin.io.path.Path

object LobbyServerBootstrap {

    private val CONFIG_PATH = Path("config.yml")

    fun run() {
        val startupStartedAt = System.nanoTime()

        try {
            val config = loadConfig()
            val minecraftServer = initMinecraftServer(config)
            val injector = createInjector(config, minecraftServer)

            runBlocking {
                injector.getInstance(LobbyServerApplication::class.java).start(startupStartedAt)
            }
        } catch (startupFailure: Throwable) {
            bootstrapLogger.error("Failed to start Surf Minestom Lobby.", startupFailure)

            // A surf feature that loaded before the failure (LuckPerms, spark, ...) is not disabled
            // by the surf api and keeps the JVM alive with its threads.
            exitFlushingLogs(1)
        }
    }

    private fun loadConfig(): ServerConfig {
        bootstrapLogger.info("Loading server configuration.")

        return ServerConfigLoader(CONFIG_PATH).load().also { config ->
            config.performance.applyTickDispatcherThreads()

            applyKeepAliveDelay()

            val disabledEntityTypes = config.performance
                .nonTickingEntityTypes
                .map { key ->
                    EntityType.fromKey(key) ?: error("Unknown entity type: $key")
                }
                .toSet()

            EntityTickFilter.configure(disabledEntityTypes)
        }
    }

    private fun initMinecraftServer(config: ServerConfig): MinecraftServer {
        bootstrapLogger.info(
            "Initializing Minestom server for {}:{}.",
            config.address.host,
            config.address.port,
        )

        val minecraftServer = MinecraftServer.init(config.createAuth())

        applyBackendCompressionThreshold()

        return minecraftServer
    }

    private fun createInjector(
        config: ServerConfig,
        minecraftServer: MinecraftServer,
    ): Injector {
        MinecraftServer.LOGGER.info("Creating dependency injector.")

        return Guice.createInjector(
            Stage.PRODUCTION,
            LobbyServerModule(
                config = config,
                minecraftServer = minecraftServer,
            ),
        )
    }
}

package dev.slne.minestom.lobby.server.bootstrap

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Stage
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.config.ServerConfigLoader
import dev.slne.minestom.lobby.server.di.LobbyServerModule
import dev.slne.minestom.lobby.server.lifecycle.LobbyServerApplication
import kotlinx.coroutines.runBlocking
import net.minestom.server.MinecraftServer
import kotlin.io.path.Path

object LobbyServerBootstrap {

    private val CONFIG_PATH = Path("config.yml")

    fun run() {
        val startupStartedAt = System.nanoTime()

        val config = ServerConfigLoader(CONFIG_PATH).also {
            bootstrapLogger.info("Loading server configuration.")
        }.load()

        // Has to be set before anything touches Minestom
        val tickThreads = config.performance.applyTickDispatcherThreads()

        val minecraftServer = initMinecraftServer(config)
        tickThreads?.let(::warnIfTickDispatcherThreadsIgnored)

        val injector = createInjector(config, minecraftServer)

        runBlocking {
            injector.getInstance(LobbyServerApplication::class.java).start(startupStartedAt)
        }
    }

    private fun initMinecraftServer(config: ServerConfig): MinecraftServer {
        bootstrapLogger.info(
            "Initializing Minestom server for {}:{}.",
            config.address.host,
            config.address.port,
        )

        return MinecraftServer.init(config.createAuth())
    }

    private fun createInjector(
        config: ServerConfig,
        minecraftServer: MinecraftServer,
    ): Injector {
        MinecraftServer.LOGGER.info("Creating dependency injector.")

        return Guice.createInjector(
            Stage.PRODUCTION,
            LobbyServerModule(config = config, minecraftServer = minecraftServer),
        )
    }
}

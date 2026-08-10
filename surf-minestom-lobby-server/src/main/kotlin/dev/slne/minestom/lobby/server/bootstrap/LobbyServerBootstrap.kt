package dev.slne.minestom.lobby.server.bootstrap

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.Stage
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.config.ServerConfigLoader
import dev.slne.minestom.lobby.server.di.LobbyServerModule
import dev.slne.minestom.lobby.server.lifecycle.LobbyServerApplication
import dev.slne.minestom.lobby.server.plugin.MinestomPluginLoader
import dev.slne.minestom.lobby.server.plugin.PluginCatalog
import kotlinx.coroutines.runBlocking
import net.minestom.server.MinecraftServer
import kotlin.io.path.Path

object LobbyServerBootstrap {

    private val CONFIG_PATH = Path("config.yml")

    fun run() {
        val startupStartedAt = System.nanoTime()

        val config = loadConfig()
        val minecraftServer = initMinecraftServer(config)
        val pluginCatalog = discoverPlugins()
        val injector = createInjector(config, minecraftServer, pluginCatalog)

        runBlocking {
            injector.getInstance(LobbyServerApplication::class.java).start(startupStartedAt)
        }
    }

    private fun loadConfig(): ServerConfig {
        bootstrapLogger.info("Loading server configuration.")

        return ServerConfigLoader(CONFIG_PATH).load().also { config ->
            config.performance.applyTickDispatcherThreads()
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

    private fun discoverPlugins(): PluginCatalog {
        MinecraftServer.LOGGER.info("Discovering server plugins.")

        val catalog = PluginCatalog(MinestomPluginLoader.discover())

        MinecraftServer.LOGGER.info("Discovered {} server plugin(s).", catalog.plugins.size)

        return catalog
    }

    private fun createInjector(
        config: ServerConfig,
        minecraftServer: MinecraftServer,
        pluginCatalog: PluginCatalog,
    ): Injector {
        MinecraftServer.LOGGER.info("Creating dependency injector.")

        val modules = buildList<Module> {
            add(
                LobbyServerModule(
                    config = config,
                    minecraftServer = minecraftServer,
                    pluginCatalog = pluginCatalog,
                )
            )

            addAll(pluginCatalog.plugins)
        }

        return Guice.createInjector(Stage.PRODUCTION, modules)
    }
}

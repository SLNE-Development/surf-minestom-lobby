package dev.slne.minestom.lobby.server.bootstrap

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.Stage
import dev.slne.minestom.lobby.api.plugin.MinestomPlugin
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.config.ServerConfigLoader
import dev.slne.minestom.lobby.server.di.LobbyServerModule
import dev.slne.minestom.lobby.server.lifecycle.LobbyServerApplication
import dev.slne.minestom.lobby.server.performance.EntityTickFilter
import dev.slne.minestom.lobby.server.plugin.MinestomPluginLoader
import dev.slne.minestom.lobby.server.plugin.PluginCatalog
import dev.slne.minestom.lobby.server.plugin.PluginModule
import kotlinx.coroutines.runBlocking
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.EntityType
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

object LobbyServerBootstrap {

    private val CONFIG_PATH = Path("config.yml")
    private val PLUGINS_PATH = Path("plugins")

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

            for (plugin in pluginCatalog.plugins) {
                add(PluginModule(plugin, createDataDirectory(plugin)))
            }
        }

        return Guice.createInjector(Stage.PRODUCTION, modules)
    }

    private fun createDataDirectory(plugin: MinestomPlugin): Path =
        PLUGINS_PATH.resolve(plugin.meta.id).createDirectories()
}

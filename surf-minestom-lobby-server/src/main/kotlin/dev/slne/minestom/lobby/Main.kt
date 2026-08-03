package dev.slne.minestom.lobby

import com.google.inject.Guice
import com.google.inject.Module
import com.google.inject.Stage
import dev.slne.minestom.lobby.server.LobbyServerApplication
import dev.slne.minestom.lobby.server.auth.createAuth
import dev.slne.minestom.lobby.server.config.ServerConfigLoader
import dev.slne.minestom.lobby.server.di.LobbyServerModule
import dev.slne.minestom.lobby.server.plugin.MinestomPluginLoader
import dev.slne.minestom.lobby.server.plugin.PluginCatalog
import kotlinx.coroutines.runBlocking
import net.minestom.server.MinecraftServer
import kotlin.io.path.Path

fun main() {
    val config = ServerConfigLoader(Path("config.yml")).load()

    val minecraftServer = MinecraftServer.init(config.createAuth())
    val pluginCatalog = PluginCatalog(MinestomPluginLoader.discover())

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

    val injector = Guice.createInjector(Stage.PRODUCTION, *modules.toTypedArray())

    runBlocking {
        injector.getInstance(LobbyServerApplication::class.java).start()
    }
}
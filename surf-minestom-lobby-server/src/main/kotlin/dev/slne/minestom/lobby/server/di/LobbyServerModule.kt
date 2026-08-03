package dev.slne.minestom.lobby.server.di

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.instance.LobbyInstance
import dev.slne.minestom.lobby.server.LobbyServerApplication
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.core.CoreServerInitializer
import dev.slne.minestom.lobby.server.plugin.MinestomPluginManager
import dev.slne.minestom.lobby.server.plugin.PluginCatalog
import dev.slne.minestom.lobby.server.world.LobbyWorldFactory
import net.minestom.server.MinecraftServer
import net.minestom.server.event.GlobalEventHandler
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.InstanceManager


class LobbyServerModule(
    private val config: ServerConfig,
    private val minecraftServer: MinecraftServer,
    private val pluginCatalog: PluginCatalog,
) : AbstractModule() {

    override fun configure() {
        bind(ServerConfig::class.java).toInstance(config)
        bind(MinecraftServer::class.java).toInstance(minecraftServer)
        bind(PluginCatalog::class.java).toInstance(pluginCatalog)

        bind(LobbyServerApplication::class.java).`in`(Singleton::class.java)
        bind(CoreServerInitializer::class.java).`in`(Singleton::class.java)
        bind(MinestomPluginManager::class.java).`in`(Singleton::class.java)
    }

    @Provides
    @Singleton
    @Suppress("UNUSED_PARAMETER")
    fun provideInstanceManager(minecraftServer: MinecraftServer): InstanceManager {
        return MinecraftServer.getInstanceManager()
    }

    @Provides
    @Singleton
    @Suppress("UNUSED_PARAMETER")
    fun provideGlobalEventHandler(minecraftServer: MinecraftServer): GlobalEventHandler {
        return MinecraftServer.getGlobalEventHandler()
    }

    @Provides
    @Singleton
    @LobbyInstance
    fun provideLobbyInstance(factory: LobbyWorldFactory): InstanceContainer {
        return factory.create()
    }
}
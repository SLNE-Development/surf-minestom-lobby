package dev.slne.minestom.lobby.server.di

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.assistedinject.FactoryModuleBuilder
import com.google.inject.multibindings.Multibinder
import dev.slne.minestom.lobby.api.command.CommandRegistrar
import dev.slne.minestom.lobby.api.instance.LobbyInstance
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import dev.slne.minestom.lobby.server.LobbyServerApplication
import dev.slne.minestom.lobby.server.command.DefaultCommandRegistrar
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.core.CoreServerInitializer
import dev.slne.minestom.lobby.server.luckperms.LuckPermsService
import dev.slne.minestom.lobby.server.player.LobbyPlayerFactory
import dev.slne.minestom.lobby.server.player.LobbyPlayerImpl
import dev.slne.minestom.lobby.server.player.LobbyPlayerService
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

        Multibinder.newSetBinder(binder(), CommandRegistrar::class.java)
            .addBinding()
            .to(DefaultCommandRegistrar::class.java)

        install(
            FactoryModuleBuilder()
                .implement(LobbyPlayer::class.java, LobbyPlayerImpl::class.java)
                .build(LobbyPlayerFactory::class.java)
        )
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
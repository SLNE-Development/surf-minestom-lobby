package dev.slne.minestom.lobby.server.di

import com.google.inject.AbstractModule
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.plugin.PluginCatalog
import net.minestom.server.MinecraftServer

class LobbyServerModule(
    private val config: ServerConfig,
    private val minecraftServer: MinecraftServer,
    private val pluginCatalog: PluginCatalog,
) : AbstractModule() {

    override fun configure() {
        bind(PluginCatalog::class.java).toInstance(pluginCatalog)

        install(MinestomModule(minecraftServer))
        install(ConfigModule(config))

        install(UploadModule())
        install(WorldModule())
        install(PlayerModule())
        install(PermissionModule())
        install(ChatModule())
        install(CommandModule())
        install(IntegrationModule())
    }
}

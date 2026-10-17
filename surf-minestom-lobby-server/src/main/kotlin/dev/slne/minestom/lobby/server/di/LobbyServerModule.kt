package dev.slne.minestom.lobby.server.di

import com.google.inject.AbstractModule
import dev.slne.minestom.lobby.server.config.ServerConfig
import net.minestom.server.MinecraftServer

class LobbyServerModule(
    private val config: ServerConfig,
    private val minecraftServer: MinecraftServer,
) : AbstractModule() {

    override fun configure() {
        install(MinestomModule(minecraftServer))
        install(ConfigModule(config))

        install(UploadModule())
        install(WorldModule())
        install(PlayerModule())
        install(PermissionModule())
    }
}

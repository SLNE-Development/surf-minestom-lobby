package dev.slne.minestom.lobby

import dev.slne.minestom.lobby.config.ServerConfigLoader
import dev.slne.minestom.lobby.config.auth.AuthProvider
import dev.slne.minestom.lobby.config.handler.GlobalEventHandlerRegistrar
import dev.slne.minestom.lobby.config.instance.LobbyInstance
import dev.slne.minestom.lobby.config.serverConfig
import net.minestom.server.MinecraftServer
import kotlin.io.path.Path

fun main() {
    ServerConfigLoader.load(Path("config.yml"))
    val server = MinecraftServer.init(AuthProvider.load())
    val instanceManager = MinecraftServer.getInstanceManager()

    val lobbyInstance = LobbyInstance.create(instanceManager)

    val globalEventHandler = MinecraftServer.getGlobalEventHandler()
    GlobalEventHandlerRegistrar.register(
        globalEventHandler,
        lobbyInstance
    )

    server.start(serverConfig.address.host, serverConfig.address.port)
}
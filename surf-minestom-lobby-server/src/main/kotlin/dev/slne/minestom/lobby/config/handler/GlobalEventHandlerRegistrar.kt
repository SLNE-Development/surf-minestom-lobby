package dev.slne.minestom.lobby.config.handler

import dev.slne.minestom.lobby.config.handler.impl.registerPlayerSpawnPositionHandler
import net.minestom.server.event.GlobalEventHandler
import net.minestom.server.instance.InstanceContainer

object GlobalEventHandlerRegistrar {

    fun register(
        handler: GlobalEventHandler,
        lobbyInstance: InstanceContainer
    ) {
        handler.registerPlayerSpawnPositionHandler(lobbyInstance)
    }
}
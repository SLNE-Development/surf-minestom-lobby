package dev.slne.minestom.lobby.config.handler.impl

import dev.slne.minestom.lobby.config.serverConfig
import net.minestom.server.event.GlobalEventHandler
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.instance.InstanceContainer

fun GlobalEventHandler.registerPlayerSpawnPositionHandler(lobbyInstance: InstanceContainer) {
    this.addListener(AsyncPlayerConfigurationEvent::class.java) { event ->
        val player = event.player
        event.spawningInstance = lobbyInstance
        player.respawnPoint = serverConfig.spawn.toPos()
    }
}
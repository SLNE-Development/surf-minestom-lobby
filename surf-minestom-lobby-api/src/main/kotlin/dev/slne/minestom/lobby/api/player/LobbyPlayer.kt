package dev.slne.minestom.lobby.api.player

import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.network.player.GameProfile
import net.minestom.server.network.player.PlayerConnection
import org.jetbrains.annotations.ApiStatus


abstract class LobbyPlayer @ApiStatus.Internal protected constructor(
    playerConnection: PlayerConnection,
    gameProfile: GameProfile,
) : Player(playerConnection, gameProfile) {

    abstract fun hasPermission(permission: String): Boolean

    fun displayName(): Component = (displayName ?: Component.text(username))
        .insertion(username)
        .hoverEvent(this)
}
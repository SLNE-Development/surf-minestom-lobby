package dev.slne.minestom.lobby.api.player

import net.kyori.adventure.chat.SignedMessage
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

    /**
     * Sends [message] to this player with its signature intact, attributed to [boundName].
     *
     * [unsignedContent] replaces what is displayed without
     * touching what was signed. Passing `null`
     * shows the signed content as it was sent.
     *
     * A message that carries no signature of this server's own is sent as plain system text.
     */
    abstract fun sendSignedMessage(
        message: SignedMessage,
        boundName: Component,
        unsignedContent: Component?,
    )

    fun displayName(): Component = (displayName ?: Component.text(username))
        .insertion(username)
        .hoverEvent(this)
}
package dev.slne.minestom.lobby.server.player

import dev.slne.minestom.lobby.api.player.LobbyPlayer
import net.minestom.server.network.player.GameProfile
import net.minestom.server.network.player.PlayerConnection


interface LobbyPlayerFactory {

    fun create(
        playerConnection: PlayerConnection,
        gameProfile: GameProfile,
    ): LobbyPlayer
}
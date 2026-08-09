package dev.slne.minestom.lobby.api.player

import net.minestom.server.entity.Player
import net.minestom.server.event.trait.PlayerEvent
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun Player.requireLobbyPlayer(): LobbyPlayer {
    contract {
        returns() implies (this@requireLobbyPlayer is LobbyPlayer)
    }

    require(this is LobbyPlayer) { "Player ${this.username} is not a LobbyPlayer" }
    return this
}

val PlayerEvent.lobbyPlayer get() = player.requireLobbyPlayer()
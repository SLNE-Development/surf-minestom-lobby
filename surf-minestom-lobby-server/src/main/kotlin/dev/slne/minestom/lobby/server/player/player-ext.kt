package dev.slne.minestom.lobby.server.player

import net.minestom.server.entity.Player
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun Player.requireLobbyPlayerImpl(): LobbyPlayerImpl {
    contract {
        returns() implies (this@requireLobbyPlayerImpl is LobbyPlayerImpl)
    }

    require(this is LobbyPlayerImpl) { "Player $username is not a LobbyPlayer" }
    return this
}

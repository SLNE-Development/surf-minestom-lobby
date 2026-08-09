package dev.slne.minestom.lobby.server.util

import com.google.common.primitives.Longs
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import dev.slne.minestom.lobby.server.player.LobbyPlayerImpl
import net.minestom.server.entity.Player
import java.util.UUID
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Vanilla's `Util.NIL_UUID`, used as the sender of system messages and as the session id of
 * unsigned message links.
 */
val NIL_UUID: UUID = UUID(0L, 0L)

fun UUID.toByteArray(): ByteArray {
    return Longs.toByteArray(this.mostSignificantBits) + Longs.toByteArray(this.leastSignificantBits)
}

@OptIn(ExperimentalContracts::class)
fun Player.requireLobbyPlayerImpl(): LobbyPlayerImpl {
    contract {
        returns() implies (this@requireLobbyPlayerImpl is LobbyPlayerImpl)
    }

    require(this is LobbyPlayerImpl) { "Player ${this.username} is not a LobbyPlayer" }
    return this
}
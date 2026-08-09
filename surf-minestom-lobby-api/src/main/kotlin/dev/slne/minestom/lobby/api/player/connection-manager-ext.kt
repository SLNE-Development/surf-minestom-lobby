@file:Suppress("UNCHECKED_CAST")

package dev.slne.minestom.lobby.api.player

import net.minestom.server.network.ConnectionManager
import net.minestom.server.network.player.PlayerConnection
import java.util.*

/**
 * @see ConnectionManager.getOnlinePlayers
 */
val ConnectionManager.onlineLobbyPlayers: Collection<LobbyPlayer>
    get() = onlinePlayers as Collection<LobbyPlayer>

/**
 * @see ConnectionManager.getConfigPlayers
 */
val ConnectionManager.configLobbyPlayers: Collection<LobbyPlayer>
    get() = configPlayers as Collection<LobbyPlayer>

/**
 * @see ConnectionManager.getPlayer
 */
fun ConnectionManager.getLobbyPlayer(
    connection: PlayerConnection,
): LobbyPlayer? = getPlayer(connection) as? LobbyPlayer

/**
 * @see ConnectionManager.getOnlinePlayerByUsername
 */
fun ConnectionManager.getOnlineLobbyPlayerByUsername(
    username: String,
): LobbyPlayer? = getOnlinePlayerByUsername(username) as? LobbyPlayer

/**
 * @see ConnectionManager.getOnlinePlayerByUuid
 */
fun ConnectionManager.getOnlineLobbyPlayerByUuid(
    uuid: UUID,
): LobbyPlayer? = getOnlinePlayerByUuid(uuid) as? LobbyPlayer

fun ConnectionManager.getLobbyPlayer(uuid: UUID): LobbyPlayer? = getOnlineLobbyPlayerByUuid(uuid)

/**
 * @see ConnectionManager.findOnlinePlayer
 */
fun ConnectionManager.findOnlineLobbyPlayer(
    username: String,
): LobbyPlayer? = findOnlinePlayer(username) as? LobbyPlayer


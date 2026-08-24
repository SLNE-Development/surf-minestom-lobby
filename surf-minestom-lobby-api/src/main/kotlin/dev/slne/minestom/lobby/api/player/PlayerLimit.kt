package dev.slne.minestom.lobby.api.player

import dev.slne.minestom.lobby.api.player.event.PlayerLoginEvent

/**
 * How many players this server admits.
 *
 * A player who would exceed [maxPlayers] is refused during login, which a listener of
 * [PlayerLoginEvent] can override.
 *
 * ```
 * class SlotListener : EventRegistrar {
 *     override fun register(node: EventNode<Event>) {
 *         node.addListener<PlayerLoginEvent> { event ->
 *             if (event.result == PlayerLoginEvent.Result.KICK_FULL &&
 *                 event.player.hasPermission("lobby.bypass-max-players")
 *             ) {
 *                 event.allow()
 *             }
 *         }
 *     }
 * }
 * ```
 */
interface PlayerLimit {

    /**
     * The number of players the server admits.
     *
     * Set from `max-players` in the server configuration. Assigning a new value applies until the
     * server stops; it is not written back to the configuration file.
     *
     * @throws IllegalArgumentException if the new value is not greater than zero
     */
    var maxPlayers: Int

    /**
     * The number of connected players, counting those still in the configuration phase.
     */
    val playerCount: Int

    /**
     * Whether [playerCount] has reached [maxPlayers].
     */
    val isFull: Boolean get() = playerCount >= maxPlayers
}

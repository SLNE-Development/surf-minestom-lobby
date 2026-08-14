package dev.slne.minestom.lobby.api.player.event

import dev.slne.minestom.lobby.api.player.LobbyPlayer
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.event.trait.PlayerInstanceEvent
import org.jetbrains.annotations.ApiStatus

class PlayerToggleFlightEvent @ApiStatus.Internal constructor(
    private val player: LobbyPlayer,

    /**
     * Whether the player is trying to start or stop flying.
     */
    val isFlying: Boolean,
) : PlayerInstanceEvent, CancellableEvent {
    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled
    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun getPlayer() = player
}
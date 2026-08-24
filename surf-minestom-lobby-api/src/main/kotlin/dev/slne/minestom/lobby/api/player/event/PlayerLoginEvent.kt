package dev.slne.minestom.lobby.api.player.event

import dev.slne.minestom.lobby.api.player.LobbyPlayer
import dev.slne.minestom.lobby.api.player.PlayerLimit
import net.kyori.adventure.text.Component
import net.minestom.server.event.trait.AsyncEvent
import net.minestom.server.event.trait.PlayerEvent
import org.jetbrains.annotations.ApiStatus

/**
 * Fired once per player, before the server configures them, to decide whether they may join.
 *
 * [result] arrives as [Result.KICK_FULL] when the player would exceed [PlayerLimit.maxPlayers] and
 * as [Result.ALLOWED] otherwise. A listener may [allow] a refused player in or [disallow] one the
 * server would have taken. A player who is refused is kicked with [kickMessage] and never reaches
 * [net.minestom.server.event.player.AsyncPlayerConfigurationEvent].
 *
 * Listeners run on the player's own virtual login thread, so they may block.
 */
class PlayerLoginEvent @ApiStatus.Internal constructor(
    private val player: LobbyPlayer,

    /**
     * Whether the player may join, and why not if they may not.
     */
    var result: Result,

    /**
     * The message a refused player is disconnected with.
     */
    var kickMessage: Component,
) : PlayerEvent, AsyncEvent {

    val isAllowed get() = result == Result.ALLOWED

    /**
     * Lets the player join, whatever an earlier listener or the server itself decided.
     */
    fun allow() {
        result = Result.ALLOWED
    }

    /**
     * Refuses the player with [result] and disconnects them with [kickMessage].
     */
    fun disallow(result: Result, kickMessage: Component) {
        require(result != Result.ALLOWED) { "disallow needs a refusing result, was $result" }

        this.result = result
        this.kickMessage = kickMessage
    }

    override fun getPlayer() = player

    enum class Result {
        ALLOWED,

        /**
         * The server has no free slot left.
         */
        KICK_FULL,

        /**
         * A listener refused the player for its own reason.
         */
        KICK_OTHER,
    }
}

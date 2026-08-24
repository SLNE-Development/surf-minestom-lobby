package dev.slne.minestom.lobby.server.player

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import dev.slne.minestom.lobby.api.player.PlayerLimit
import dev.slne.minestom.lobby.api.player.event.PlayerLoginEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.event.EventDispatcher

/**
 * Decides whether a player who just logged in may stay.
 */
@Singleton
class PlayerLoginGate @Inject constructor(private val playerLimit: PlayerLimit) {

    /**
     * Fires [PlayerLoginEvent] for [player] and kicks them unless a slot is theirs.
     *
     * [player] already counts towards [PlayerLimit.playerCount] when this runs, so the server is
     * over its limit exactly when the count exceeds it.
     *
     * @return whether the player may join
     */
    fun admit(player: LobbyPlayer): Boolean {
        val event = PlayerLoginEvent(player, initialResult(), SERVER_FULL)
        EventDispatcher.call(event)

        if (event.isAllowed) return true

        player.kick(event.kickMessage)
        return false
    }

    private fun initialResult() = when {
        playerLimit.playerCount > playerLimit.maxPlayers -> PlayerLoginEvent.Result.KICK_FULL
        else -> PlayerLoginEvent.Result.ALLOWED
    }

    private companion object {
        val SERVER_FULL: Component = text("Der Server ist voll.", NamedTextColor.RED)
    }
}

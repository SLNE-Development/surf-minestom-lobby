package dev.slne.minestom.lobby.server.player

import dev.slne.surf.api.minestom.server.SurfMinestomServer
import dev.slne.surf.api.minestom.server.visibility.PlayerVisibilityFeature
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.potion.PotionEffect
import java.util.function.Predicate

private val ALWAYS_VIEWABLE = Predicate<Player> { true }

/**
 * Completes a switch from [previousGameMode] to the player's current [Player.getGameMode] with the
 * parts of the vanilla switch Minestom leaves out.
 */
internal fun Player.completeGameModeSwitch(previousGameMode: GameMode) {
    val isSpectator = gameMode == GameMode.SPECTATOR
    val wasSpectator = previousGameMode == GameMode.SPECTATOR

    if (isSpectator == wasSpectator) return

    if (isSpectator) {
        vehicle?.removePassenger(this)
        stopItemUse()
        isInvisible = true
        updateViewableRule { viewer -> viewer.gameMode == GameMode.SPECTATOR }
    } else {
        if (isActive) {
            stopSpectating()
        }

        isInvisible = hasEffect(PotionEffect.INVISIBILITY)
        updateViewableRule(defaultViewableRule())
    }

    refreshSpectatorRules()
}

/**
 * The viewable rule a player has outside of spectator mode: the one the player visibility feature
 * installs while it limits the visible players, none otherwise.
 */
private fun defaultViewableRule(): Predicate<Player>? {
    val surfApi = runCatching { SurfMinestomServer.instance }.getOrNull() ?: return null
    return ALWAYS_VIEWABLE.takeIf { surfApi.feature(PlayerVisibilityFeature.ID) != null }
}

/** Re-runs the rule of every other spectator, which decides whether this player may see them. */
private fun Player.refreshSpectatorRules() {
    val instance = instance ?: return

    for (player in instance.players) {
        if (player !== this && player.gameMode == GameMode.SPECTATOR) {
            player.updateViewableRule()
        }
    }
}

private fun Player.stopItemUse() {
    val hand = itemUseHand ?: return

    refreshActiveHand(false, hand == PlayerHand.OFF, false)
    clearItemUse()
}

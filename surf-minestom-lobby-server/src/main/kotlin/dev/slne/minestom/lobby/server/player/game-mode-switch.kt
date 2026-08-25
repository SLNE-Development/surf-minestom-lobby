package dev.slne.minestom.lobby.server.player

import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.potion.PotionEffect

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
        updateViewableRule(null)
    }

    refreshSpectatorRules()
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

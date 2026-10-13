package dev.slne.minestom.lobby.server.player

import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.network.player.GameProfile
import net.minestom.server.network.player.PlayerConnection

/**
 * The player of the lobby, which completes game mode switches the way vanilla does.
 *
 * Installed as the player provider by [LobbyPlayerService].
 */
class LobbyPlayer(
    playerConnection: PlayerConnection,
    gameProfile: GameProfile,
) : Player(playerConnection, gameProfile) {

    override fun setGameMode(gameMode: GameMode): Boolean {
        val previousGameMode = getGameMode()
        if (!super.setGameMode(gameMode)) return false

        completeGameModeSwitch(previousGameMode)
        return true
    }
}

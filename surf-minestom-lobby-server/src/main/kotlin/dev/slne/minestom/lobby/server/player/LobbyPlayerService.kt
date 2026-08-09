package dev.slne.minestom.lobby.server.player

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.extension.ConnectionManager

@Singleton
class LobbyPlayerService @Inject constructor(
    private val playerFactory: LobbyPlayerFactory,
) {

    fun registerPlayerProvider() {
        ConnectionManager.setPlayerProvider(playerFactory::create)
    }
}
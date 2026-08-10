package dev.slne.minestom.lobby.server.player

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.extension.ConnectionManager
import dev.slne.minestom.lobby.server.lifecycle.LobbyService

@Singleton
class LobbyPlayerService @Inject constructor(
    private val playerFactory: LobbyPlayerFactory,
) : LobbyService {

    override suspend fun start() {
        ConnectionManager.setPlayerProvider(playerFactory::create)
    }
}

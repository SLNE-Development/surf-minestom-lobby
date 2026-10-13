package dev.slne.minestom.lobby.server.player

import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.lifecycle.LobbyService
import dev.slne.surf.api.minestom.extension.ConnectionManager

@Singleton
class LobbyPlayerService : LobbyService {

    override suspend fun start() {
        ConnectionManager.setPlayerProvider(::LobbyPlayer)
    }

    override suspend fun stop() {
        ConnectionManager.setPlayerProvider(null)
    }
}

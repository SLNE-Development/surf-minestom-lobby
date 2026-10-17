package dev.slne.minestom.lobby.server.command

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.lifecycle.LobbyService


@Singleton
class CommandService @Inject constructor(
    private val registrar: DefaultCommandRegistrar,
) : LobbyService {

    override suspend fun start() {
        registrar.register()
    }
}

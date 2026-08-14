package dev.slne.minestom.lobby.server.command

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.command.CommandRegistrar
import dev.slne.minestom.lobby.server.lifecycle.LobbyService


@Singleton
class CommandService @Inject constructor(
    private val registrars: Set<@JvmSuppressWildcards CommandRegistrar>,
) : LobbyService {

    override suspend fun start() {
        for (registrar in registrars) {
            registrar.register()
        }
    }
}

package dev.slne.minestom.lobby.server.command

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.command.CommandRegistrar
import dev.slne.minestom.lobby.api.extension.CommandManager
import dev.slne.minestom.lobby.server.command.commandapi.CommandAPIHook
import dev.slne.minestom.lobby.server.lifecycle.LobbyService
import net.minestom.server.utils.callback.CommandCallback


@Singleton
class CommandService @Inject constructor(
    private val registrars: Set<@JvmSuppressWildcards CommandRegistrar>,
) : LobbyService {

    override suspend fun start() {
        for (registrar in registrars) {
            registrar.register()
        }

        CommandManager.unknownCommandCallback = CommandCallback(CommandAPIHook::reportUnknown)
    }
}

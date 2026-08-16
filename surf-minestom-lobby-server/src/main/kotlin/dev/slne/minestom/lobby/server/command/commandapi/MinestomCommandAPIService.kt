package dev.slne.minestom.lobby.server.command.commandapi

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.command.commandapi.CommandAPI
import dev.slne.minestom.lobby.api.extension.CommandManager
import dev.slne.minestom.lobby.server.lifecycle.LobbyService

@Singleton
class MinestomCommandAPIService @Inject constructor(
    private val ownership: MinestomCommandOwnership,
) : LobbyService {
    private var platform: MinestomCommandAPIPlatform? = null

    override suspend fun start() {
        check(platform == null) { "Minestom CommandAPI is already installed" }

        CommandAPITranslations.register()

        val installed = MinestomCommandAPIPlatform(CommandManager, ownership)
        CommandAPI.installPlatform(installed)
        platform = installed
    }

    override suspend fun stop() {
        val installed = platform ?: return
        platform = null
        try {
            installed.close()
        } finally {
            CommandAPI.uninstallPlatform(installed)
        }
    }
}

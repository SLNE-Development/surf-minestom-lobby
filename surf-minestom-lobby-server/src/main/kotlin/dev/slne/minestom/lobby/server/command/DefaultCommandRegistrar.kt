package dev.slne.minestom.lobby.server.command

import com.google.inject.Inject
import com.google.inject.Provider
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.command.CommandRegistrar
import dev.slne.minestom.lobby.server.command.impl.*
import dev.slne.minestom.lobby.server.lifecycle.LobbyServerApplication
import dev.slne.minestom.lobby.server.upload.UploadService
import dev.slne.minestom.lobby.server.version.LobbyVersionService

@Singleton
class DefaultCommandRegistrar @Inject constructor(
    private val lobbyServerApplication: Provider<LobbyServerApplication>,
    private val uploads: UploadService,
    private val versionService: LobbyVersionService,
) : CommandRegistrar {

    override fun register() {
        gamemodeCommand()
        killCommand()
        listPlayersCommand()
        kickCommand()
        difficultyCommand()
        stopCommand(lobbyServerApplication)
        uploadsCommand(uploads)
        versionCommand(versionService)
    }
}

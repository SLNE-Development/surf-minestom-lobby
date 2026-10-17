package dev.slne.minestom.lobby.server.command

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.command.impl.*
import dev.slne.minestom.lobby.server.upload.UploadService
import dev.slne.minestom.lobby.server.version.LobbyVersionService

/**
 * Registers the lobby's own commands. The CommandAPI they are built with is ready once
 * `surfMinestomServer { }` returned.
 */
@Singleton
class DefaultCommandRegistrar @Inject constructor(
    private val uploads: UploadService,
    private val versionService: LobbyVersionService,
) {

    fun register() {
        gamemodeCommand()
        killCommand()
        listPlayersCommand()
        kickCommand()
        difficultyCommand()
        stopCommand()
        uploadsCommand(uploads)
        versionCommand(versionService)
    }
}

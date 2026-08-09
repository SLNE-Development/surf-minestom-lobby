package dev.slne.minestom.lobby.server.command.impl

import dev.slne.minestom.lobby.api.command.CommandPermission
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import revxrsal.commands.annotation.Command

@Command("version")
@CommandPermission(LobbyPermissions.VERSION_COMMAND)
class VersionCommand { // TODO: implement
}
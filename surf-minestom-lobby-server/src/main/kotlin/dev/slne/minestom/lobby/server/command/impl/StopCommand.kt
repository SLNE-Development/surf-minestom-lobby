package dev.slne.minestom.lobby.server.command.impl

import com.google.inject.Provider
import dev.slne.minestom.lobby.api.command.commandapi.dsl.anyExecutor
import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandAPICommand
import dev.slne.minestom.lobby.server.lifecycle.LobbyServerApplication
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor

fun stopCommand(lobbyServerApplication: Provider<LobbyServerApplication>) =
    commandAPICommand("stop") {
        withPermission(LobbyPermissions.STOP_COMMAND)

        anyExecutor { sender, _ ->
            sender.sendMessage(text("Der Server wird heruntergefahren...", NamedTextColor.GRAY))

            lobbyServerApplication.get().beginShutdown()
        }
    }

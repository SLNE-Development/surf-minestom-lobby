package dev.slne.minestom.lobby.server.command.impl

import com.google.inject.Provider
import dev.slne.minestom.lobby.api.command.commandapi.dsl.anyExecutor
import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandAPICommand
import dev.slne.minestom.lobby.server.lifecycle.LobbyServerApplication
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import dev.slne.surf.api.core.messages.adventure.sendText

fun stopCommand(lobbyServerApplication: Provider<LobbyServerApplication>) =
    commandAPICommand("stop") {
        withPermission(LobbyPermissions.STOP_COMMAND)

        anyExecutor { sender, _ ->
            sender.sendText {
                appendInfoPrefix()
                info("Der Server wird heruntergefahren...")
            }

            lobbyServerApplication.get().beginShutdown()
        }
    }

package dev.slne.minestom.lobby.server.command.impl

import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.command.dsl.commandAPICommand
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.minestom.server.SurfMinestomServer

/**
 * Registers `/stop`, which runs [exit] after telling the sender.
 *
 * [SurfMinestomServer.exit] stops and exits on a thread of its own, so it may run on the tick
 * thread of a player.
 */
fun stopCommand(exit: () -> Unit = { SurfMinestomServer.instance.exit() }) =
    commandAPICommand("stop") {
        withPermission(LobbyPermissions.STOP_COMMAND)

        anyExecutor { sender, _ ->
            sender.sendText {
                appendInfoPrefix()
                info("Der Server wird heruntergefahren...")
            }

            exit()
        }
    }

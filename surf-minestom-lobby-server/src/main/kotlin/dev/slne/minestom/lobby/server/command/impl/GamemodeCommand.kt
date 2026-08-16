package dev.slne.minestom.lobby.server.command.impl

import dev.slne.minestom.lobby.api.command.commandapi.dsl.*
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.CommandSender
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player

fun gamemodeCommand() = commandTree("gamemode") {
    withAliases("gm")
    withPermission(LobbyPermissions.GAMEMODE_COMMAND)

    gameModeArgument("gamemode") {
        playerExecutor { player, args ->
            applyGameMode(player, args.get("gamemode"), listOf(player))
        }

        playersArgument("target") {
            anyExecutor { sender, args ->
                applyGameMode(sender, args.get("gamemode"), args.get("target"))
            }
        }
    }
}

private fun applyGameMode(sender: CommandSender, gameMode: GameMode, targets: List<Player>) {
    val selfExecution = targets.size == 1 && targets.first() == sender
    val displayMode = text(gameMode.displayName, NamedTextColor.GOLD)

    for (player in targets) {
        player.scheduleNextTick { player.gameMode = gameMode }

        if (!selfExecution) {
            player.sendMessage(
                text("Dein Spielmodus wurde auf ", NamedTextColor.GRAY)
                    .append(displayMode)
                    .append(text(" gesetzt.", NamedTextColor.GRAY))
            )
        }
    }

    if (selfExecution) {
        sender.sendMessage(
            text("Dein Spielmodus wurde auf ", NamedTextColor.GRAY)
                .append(displayMode)
                .append(text(" gesetzt.", NamedTextColor.GRAY))
        )
    } else {
        sender.sendMessage(
            text("Der Spielmodus von ", NamedTextColor.GRAY)
                .append {
                    if (targets.size == 1) {
                        text(targets.first().username, NamedTextColor.GOLD)
                    } else {
                        text(targets.size, NamedTextColor.GOLD)
                            .append(text(" Spielern", NamedTextColor.GRAY))
                    }
                }
                .append(text(" wurde auf ", NamedTextColor.GRAY))
                .append(displayMode)
                .append(text(" gesetzt.", NamedTextColor.GRAY))
        )
    }
}

private val GameMode.displayName: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }

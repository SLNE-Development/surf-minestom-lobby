package dev.slne.minestom.lobby.server.command.impl

import dev.slne.minestom.lobby.api.command.commandapi.dsl.anyExecutor
import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandTree
import dev.slne.minestom.lobby.api.command.commandapi.dsl.gameModeArgument
import dev.slne.minestom.lobby.api.command.commandapi.dsl.playerArgument
import dev.slne.minestom.lobby.api.command.commandapi.dsl.playerExecutor
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
            applyGameMode(player, args.get("gamemode"), player)
        }

        playerArgument("target") {
            anyExecutor { sender, args ->
                applyGameMode(sender, args.get("gamemode"), args.get("target"))
            }
        }
    }
}

private fun applyGameMode(sender: CommandSender, gameMode: GameMode, target: Player) {
    target.gameMode = gameMode

    val displayMode = text(gameMode.displayName, NamedTextColor.GOLD)
    if (target == sender) {
        sender.sendMessage(
            text("Dein Spielmodus wurde auf ", NamedTextColor.GRAY)
                .append(displayMode)
                .append(text(" gesetzt.", NamedTextColor.GRAY))
        )
    } else {
        sender.sendMessage(
            text("Der Spielmodus von ", NamedTextColor.GRAY)
                .append(text(target.username, NamedTextColor.GOLD))
                .append(text(" wurde auf ", NamedTextColor.GRAY))
                .append(displayMode)
                .append(text(" gesetzt.", NamedTextColor.GRAY))
        )
        target.sendMessage(
            text("Dein Spielmodus wurde auf ", NamedTextColor.GRAY)
                .append(displayMode)
                .append(text(" gesetzt.", NamedTextColor.GRAY))
        )
    }
}

private val GameMode.displayName: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }

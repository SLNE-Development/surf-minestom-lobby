package dev.slne.minestom.lobby.server.command.impl

import dev.slne.minestom.lobby.api.command.commandapi.dsl.*
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import dev.slne.surf.api.core.messages.adventure.buildText
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
            player.sendMessage(buildText {
                appendSuccessPrefix()
                success("Dein Spielmodus wurde auf ")
                append(displayMode)
                success(" gesetzt.")
            })
        }
    }

    if (selfExecution) {
        sender.sendMessage(buildText {
            appendSuccessPrefix()
            success("Dein Spielmodus wurde auf ")
            append(displayMode)
            success(" gesetzt.")
        })
    } else {
        sender.sendMessage(buildText {
            appendSuccessPrefix()
            success("Der Spielmodus von ")
            if (targets.size == 1) {
                append(text(targets.first().username, NamedTextColor.GOLD))
            } else {
                append(text(targets.size, NamedTextColor.GOLD))
                append(text(" Spielern", NamedTextColor.GRAY))
            }
            success(" wurde auf ")
            append(displayMode)
            success(" gesetzt.")
        })
    }
}

private val GameMode.displayName: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }

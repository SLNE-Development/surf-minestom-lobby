package dev.slne.minestom.lobby.server.command.impl

import dev.slne.minestom.lobby.api.command.commandapi.argument.GreedyStringArgument
import dev.slne.minestom.lobby.api.command.commandapi.argument.PlayersArgument
import dev.slne.minestom.lobby.api.command.commandapi.dsl.anyExecutor
import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandAPICommand
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.Player

private const val DEFAULT_KICK_REASON = "Du wurdest vom Server gekickt."

fun kickCommand() = commandAPICommand("kick") {
    withPermission(LobbyPermissions.KICK_COMMAND)
    withArguments(PlayersArgument("targets"))
    withOptionalArguments(GreedyStringArgument("reason"))

    anyExecutor { sender, args ->
        val targets: List<Player> = args.get("targets")
        val reason = args.getOptional<String>("reason") ?: DEFAULT_KICK_REASON

        for (player in targets) {
            player.scheduleNextTick {
                player.kick(text(reason))
            }
        }

        sender.sendMessage(
            text()
                .append(text("Du hast ", NamedTextColor.GRAY))
                .append(text("${targets.size} Spieler", NamedTextColor.GOLD))
                .append(text(" gekickt.", NamedTextColor.GRAY))
        )
    }
}

package dev.slne.minestom.lobby.server.command.impl

import dev.slne.minestom.lobby.api.command.commandapi.dsl.anyExecutor
import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandAPICommand
import dev.slne.minestom.lobby.api.extension.ConnectionManager
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor

fun listPlayersCommand() = commandAPICommand("list") {
    withPermission(LobbyPermissions.LIST_COMMAND)

    anyExecutor { sender, _ ->
        val players = ConnectionManager.onlinePlayers
        val size = players.size

        val joined = Component.join(
            JoinConfiguration.commas(true),
            players.map { player ->
                text()
                    .append(text(player.username, NamedTextColor.GOLD))
                    .hoverEvent(text("UUID: ${player.uuid}", NamedTextColor.GRAY))
                    .clickEvent(ClickEvent.copyToClipboard(player.uuid.toString()))
                    .insertion(player.username)
            }
        )

        sender.sendMessage(
            text()
                .append(text("Es ", NamedTextColor.GRAY))
                .append {
                    if (size == 1) {
                        text("ist ", NamedTextColor.GRAY)
                    } else {
                        text("sind ", NamedTextColor.GRAY)
                    }
                }
                .append(text(" $size ", NamedTextColor.GOLD))
                .append(text("Spieler online: ", NamedTextColor.GRAY))
                .append(joined)
        )
    }
}

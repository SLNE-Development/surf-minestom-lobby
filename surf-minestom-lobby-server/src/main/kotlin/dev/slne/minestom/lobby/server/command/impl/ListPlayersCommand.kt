package dev.slne.minestom.lobby.server.command.impl

import dev.slne.minestom.lobby.api.command.CommandPermission
import dev.slne.minestom.lobby.api.extension.ConnectionManager
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.minestom.actor.MinestomCommandActor

@Command("list")
@CommandPermission(LobbyPermissions.LIST_COMMAND)
class ListPlayersCommand {

    @CommandPlaceholder
    fun list(actor: MinestomCommandActor) {
        val players = ConnectionManager.onlinePlayers
        val size = players.size

        val joined = Component.join(
            JoinConfiguration.commas(true),
            players.map { player ->
                text()
                    .append(
                        player.displayName?.colorIfAbsent(NamedTextColor.GOLD)
                            ?: text(player.username, NamedTextColor.GOLD)
                    )
                    .hoverEvent(text("UUID: ${player.uuid}", NamedTextColor.GRAY))
                    .clickEvent(ClickEvent.copyToClipboard(player.uuid.toString()))
                    .insertion(player.username)
            }
        )

        actor.reply(
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
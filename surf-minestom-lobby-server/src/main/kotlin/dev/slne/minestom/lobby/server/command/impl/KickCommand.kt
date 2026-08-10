package dev.slne.minestom.lobby.server.command.impl

import dev.slne.minestom.lobby.api.command.CommandPermission
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.Player
import net.minestom.server.utils.entity.EntityFinder
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.minestom.actor.MinestomCommandActor

@Command("kick")
@CommandPermission(LobbyPermissions.KICK_COMMAND)
class KickCommand {

    @CommandPlaceholder
    fun kick(
        actor: MinestomCommandActor,
        targets: EntityFinder,
        reason: String? = null
    ) {
        val toKick = targets.find(actor.sender()).filterIsInstance<Player>()
        val reason = reason ?: "Du wurdest vom Server gekickt."

        for (player in toKick) {
            player.scheduleNextTick {
                player.kick(text(reason))
            }
        }

        actor.reply(
            text()
                .append(text("Du hast ", NamedTextColor.GRAY))
                .append(text("${toKick.size} Spieler", NamedTextColor.GOLD))
                .append(text(" gekickt.", NamedTextColor.GRAY))
        )
    }
}
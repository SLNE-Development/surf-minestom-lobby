package dev.slne.minestom.lobby.server.command.impl

import dev.slne.minestom.lobby.api.command.CommandPermission
import dev.slne.minestom.lobby.api.command.selector.PlayerTargets
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.minestom.actor.MinestomCommandActor

@Command("kick")
@CommandPermission(LobbyPermissions.KICK_COMMAND)
class KickCommand {

    @CommandPlaceholder
    fun kick(
        actor: MinestomCommandActor,
        targets: PlayerTargets,
        reason: String? = null
    ) {
        val reason = reason ?: "Du wurdest vom Server gekickt."

        for (player in targets) {
            player.scheduleNextTick {
                player.kick(text(reason))
            }
        }

        actor.reply(
            text()
                .append(text("Du hast ", NamedTextColor.GRAY))
                .append(text("${targets.size} Spieler", NamedTextColor.GOLD))
                .append(text(" gekickt.", NamedTextColor.GRAY))
        )
    }
}

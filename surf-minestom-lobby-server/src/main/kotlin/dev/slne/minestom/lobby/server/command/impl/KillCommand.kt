package dev.slne.minestom.lobby.server.command.impl

import dev.slne.minestom.lobby.api.command.commandapi.dsl.anyExecutor
import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandTree
import dev.slne.minestom.lobby.api.command.commandapi.dsl.entitiesArgument
import dev.slne.minestom.lobby.api.command.commandapi.dsl.playerExecutor
import dev.slne.minestom.lobby.api.command.entity.displayName
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.CommandSender
import net.minestom.server.entity.Entity
import net.minestom.server.entity.LivingEntity

fun killCommand() = commandTree("kill") {
    withPermission(LobbyPermissions.KILL_COMMAND)

    playerExecutor { player, _ ->
        kill(player, listOf(player))
    }

    entitiesArgument("targets") {
        anyExecutor { sender, args ->
            kill(sender, args.get("targets"))
        }
    }
}

private fun kill(sender: CommandSender, victims: List<Entity>) {
    victims.forEach { victim ->
        victim.scheduleNextTick { entity ->
            if (entity is LivingEntity) entity.kill() else entity.remove()
        }
    }

    if (victims.size == 1) {
        sender.sendMessage(
            text()
                .append(victims.first().displayName.colorIfAbsent(NamedTextColor.GOLD))
                .appendSpace()
                .append(text("wurde getötet!", NamedTextColor.GRAY))
        )
    } else {
        sender.sendMessage(
            text()
                .append(text("${victims.size} Entitäten", NamedTextColor.GOLD))
                .appendSpace()
                .append(text("wurden getötet!", NamedTextColor.GRAY))
        )
    }
}

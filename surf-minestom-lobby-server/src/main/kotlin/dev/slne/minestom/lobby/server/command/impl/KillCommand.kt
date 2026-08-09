package dev.slne.minestom.lobby.server.command.impl

import dev.slne.minestom.lobby.api.command.CommandPermission
import dev.slne.minestom.lobby.api.command.entity.displayName
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.Entity
import net.minestom.server.entity.LivingEntity
import net.minestom.server.utils.entity.EntityFinder
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.minestom.actor.MinestomCommandActor

@Command("kill")
@CommandPermission(LobbyPermissions.KILL_COMMAND)
class KillCommand {

    @CommandPlaceholder
    fun self(actor: MinestomCommandActor) {
        kill(actor, listOf(actor.requirePlayer()))
    }

    @CommandPlaceholder
    fun target(actor: MinestomCommandActor, targets: EntityFinder)  {
        val victims = targets.find(actor.sender())
        kill(actor, victims)
    }

    private fun kill(actor: MinestomCommandActor, victims: List<Entity>) {
        victims.forEach { victim ->
            victim.scheduleNextTick { entity ->
                if (entity is LivingEntity) entity.kill() else entity.remove()
            }
        }

        if (victims.size == 1) {
            actor.sendRawMessage(
                text()
                    .append(victims.first().displayName.colorIfAbsent(NamedTextColor.GOLD))
                    .appendSpace()
                    .append(text("wurde getötet!", NamedTextColor.GRAY))
            )
        } else {
            actor.sendRawMessage(
                text()
                    .append(text("${victims.size} Entitäten", NamedTextColor.GOLD))
                    .appendSpace()
                    .append(text("wurden getötet!", NamedTextColor.GRAY))
            )
        }
    }
}
package dev.slne.minestom.lobby.server.command.impl

import dev.slne.minestom.lobby.api.command.CommandPermission
import dev.slne.minestom.lobby.api.command.args.LiteralEnum
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.world.Difficulty
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.minestom.actor.MinestomCommandActor

@Command("difficulty")
@CommandPermission(LobbyPermissions.DIFFICULTY_COMMAND)
class DifficultyCommand {

    @CommandPlaceholder
    fun difficulty(actor: MinestomCommandActor, @LiteralEnum difficulty: Difficulty) {
        MinecraftServer.setDifficulty(difficulty)

        actor.reply(
            text("Die Schwierigkeit wurde auf ", NamedTextColor.GRAY)
                .append(
                    text(
                        difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
                        NamedTextColor.GOLD
                    )
                )
                .append(text(" gesetzt.", NamedTextColor.GRAY))
        )
    }
}

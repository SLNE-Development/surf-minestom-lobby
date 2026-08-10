package dev.slne.minestom.lobby.server.command.impl

import dev.slne.minestom.lobby.api.command.CommandPermission
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import dev.slne.minestom.lobby.api.player.asLobbyPlayer
import dev.slne.minestom.lobby.api.player.requireLobbyPlayer
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.GameMode
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.minestom.actor.MinestomCommandActor

@Command("gamemode", "gm")
@CommandPermission(LobbyPermissions.GAMEMODE_COMMAND)
class GamemodeCommand {

    @CommandPlaceholder
    fun self(actor: MinestomCommandActor, gameMode: GameMode) {
        applyGameMode(actor, gameMode, actor.requireLobbyPlayer())
    }

    @CommandPlaceholder
    fun other(
        actor: MinestomCommandActor,
        gameMode: GameMode,
        target: LobbyPlayer,
    ) {
        applyGameMode(actor, gameMode, target)
    }

    private fun applyGameMode(
        actor: MinestomCommandActor,
        gameMode: GameMode,
        target: LobbyPlayer
    ) {
        target.gameMode = gameMode

        val displayMode = text(gameMode.displayName, NamedTextColor.GOLD)
        if (target == actor.asLobbyPlayer()) {
            actor.reply(
                text("Dein Spielmodus wurde auf ", NamedTextColor.GRAY)
                    .append(displayMode)
                    .append(text(" gesetzt.", NamedTextColor.GRAY))
            )
        } else {
            actor.reply(
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
}

package dev.slne.minestom.lobby.server.command.impl

import dev.slne.minestom.lobby.api.command.commandapi.dsl.anyExecutor
import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandTree
import dev.slne.minestom.lobby.api.command.commandapi.dsl.multiLiteralArgument
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.world.Difficulty


fun difficultyCommand() = commandTree("difficulty") {
    withPermission(LobbyPermissions.DIFFICULTY_COMMAND)

    anyExecutor { sender, _ ->
        val current = MinecraftServer.getDifficulty()
        sender.sendMessage(
            text("Die aktuelle Schwierigkeit ist ", NamedTextColor.GRAY)
                .append(
                    text(
                        current.name.lowercase().replaceFirstChar { it.uppercase() },
                        NamedTextColor.GOLD
                    )
                )
        )
    }

    multiLiteralArgument(
        "difficulty",
        *Difficulty.entries.map { it.toString().lowercase() }.toTypedArray()
    ) {
        anyExecutor { sender, arguments ->
            val difficulty = Difficulty.valueOf(arguments.get<String>("difficulty").uppercase())
            MinecraftServer.setDifficulty(difficulty)
            sender.sendMessage(
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
}
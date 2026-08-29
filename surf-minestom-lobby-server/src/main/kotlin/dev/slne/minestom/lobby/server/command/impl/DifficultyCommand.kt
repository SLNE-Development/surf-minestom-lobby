package dev.slne.minestom.lobby.server.command.impl

import dev.slne.minestom.lobby.api.command.commandapi.dsl.anyExecutor
import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandTree
import dev.slne.minestom.lobby.api.command.commandapi.dsl.multiLiteralArgument
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import dev.slne.surf.api.core.messages.adventure.sendText
import net.minestom.server.MinecraftServer
import net.minestom.server.world.Difficulty


fun difficultyCommand() = commandTree("difficulty") {
    withPermission(LobbyPermissions.DIFFICULTY_COMMAND)

    anyExecutor { sender, _ ->
        val current = MinecraftServer.getDifficulty()
        sender.sendText {
            appendInfoPrefix()
            info("Die aktuelle Schwierigkeit ist ")
            variableValue(current.name.lowercase().replaceFirstChar { it.uppercase() })
        }
    }

    multiLiteralArgument(
        "difficulty",
        *Difficulty.entries.map { it.toString().lowercase() }.toTypedArray()
    ) {
        anyExecutor { sender, arguments ->
            val difficulty = Difficulty.valueOf(arguments.get<String>("difficulty").uppercase())
            MinecraftServer.setDifficulty(difficulty)


            sender.sendText {
                appendSuccessPrefix()
                success("Die Schwierigkeit wurde auf ")
                variableValue(difficulty.name.lowercase().replaceFirstChar { it.uppercase() })
                success(" gesetzt.")
            }
        }
    }
}
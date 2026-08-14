package dev.slne.minestom.lobby.server.command

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.command.CommandRegistrar
import dev.slne.minestom.lobby.server.command.impl.*
import revxrsal.commands.Lamp
import revxrsal.commands.minestom.actor.MinestomCommandActor

@Singleton
class DefaultCommandRegistrar @Inject constructor(
    private val gamemode: GamemodeCommand,
    private val kill: KillCommand,
    private val list: ListPlayersCommand,
    private val stop: StopCommand,
    private val kick: KickCommand,
) : CommandRegistrar {

    override fun register(lamp: Lamp<MinestomCommandActor>) {
        lamp.register(gamemode, kill, list, stop, kick)

        difficultyCommand()
    }
}
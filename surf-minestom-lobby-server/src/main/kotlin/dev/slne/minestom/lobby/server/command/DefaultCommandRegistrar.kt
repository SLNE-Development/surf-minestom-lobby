package dev.slne.minestom.lobby.server.command

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.command.CommandRegistrar
import dev.slne.minestom.lobby.server.command.impl.*
import revxrsal.commands.Lamp
import revxrsal.commands.minestom.actor.MinestomCommandActor

@Singleton
class DefaultCommandRegistrar @Inject constructor(
    private val stopCommand: StopCommand
) : CommandRegistrar {
    override fun register(lamp: Lamp<MinestomCommandActor>) {
        lamp.register(GamemodeCommand())
        lamp.register(DifficultyCommand())
        lamp.register(KillCommand())
        lamp.register(ListPlayersCommand())
        lamp.register(stopCommand)
        lamp.register(KickCommand())
    }
}
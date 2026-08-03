package dev.slne.minestom.lobby.server.command

import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.command.CommandRegistrar
import revxrsal.commands.Lamp
import revxrsal.commands.minestom.actor.MinestomCommandActor

@Singleton
class DefaultCommandRegistrar : CommandRegistrar {
    override fun register(lamp: Lamp<MinestomCommandActor>) {

    }
}
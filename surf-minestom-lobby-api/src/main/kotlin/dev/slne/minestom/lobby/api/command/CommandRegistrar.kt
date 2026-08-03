package dev.slne.minestom.lobby.api.command

import revxrsal.commands.Lamp
import revxrsal.commands.minestom.actor.MinestomCommandActor

interface CommandRegistrar {

    fun register(lamp: Lamp<MinestomCommandActor>)
}
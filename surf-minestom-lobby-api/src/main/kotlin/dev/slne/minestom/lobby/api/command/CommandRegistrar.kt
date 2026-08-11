package dev.slne.minestom.lobby.api.command

import revxrsal.commands.Lamp
import revxrsal.commands.LampBuilderVisitor
import revxrsal.commands.minestom.actor.MinestomCommandActor

/**
 * Registers commands after Lamp has been built.
 *
 * Implement [LampBuilderVisitor] as well to contribute parameter types or other Lamp
 * configuration before this registrar receives the finished instance.
 * Implement [MinestomLampConfigVisitor] when the contribution must happen at the native
 * Minestom configuration layer, for example to register an argument type factory.
 */
interface CommandRegistrar {

    fun register(lamp: Lamp<MinestomCommandActor>)
}

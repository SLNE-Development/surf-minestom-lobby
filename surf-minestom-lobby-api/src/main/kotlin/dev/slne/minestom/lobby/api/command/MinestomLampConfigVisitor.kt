package dev.slne.minestom.lobby.api.command

import revxrsal.commands.minestom.MinestomLampConfig
import revxrsal.commands.minestom.actor.MinestomCommandActor

/**
 * Contributes native Minestom argument types before the shared Lamp instance is built.
 *
 * Implement this alongside [CommandRegistrar] when a command parameter must use a native
 * Minecraft parser instead of Lamp's generic string adapter.
 */
fun interface MinestomLampConfigVisitor {
    fun configure(builder: MinestomLampConfig.Builder<MinestomCommandActor>)
}

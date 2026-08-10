package dev.slne.minestom.lobby.api.command.selector

import dev.slne.minestom.lobby.api.player.LobbyPlayer
import java.util.function.IntFunction

/**
 * The players resolved by a single Minecraft target-selector argument.
 */
class PlayerTargets private constructor(
    private val delegate: List<LobbyPlayer>,
) : List<LobbyPlayer> by delegate {

    constructor(players: Collection<LobbyPlayer>) : this(players.toList())

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "OVERRIDE_DEPRECATION")
    override fun <T> toArray(generator: IntFunction<Array<out T>>): Array<T> {
        return (delegate as java.util.Collection<*>).toArray(generator)
    }
}

package dev.slne.minestom.lobby.api.command.selector

import net.minestom.server.entity.Entity
import java.util.function.IntFunction

/**
 * The entities resolved by a single Minecraft target-selector argument.
 */
class EntityTargets private constructor(
    private val delegate: List<Entity>,
) : List<Entity> by delegate {

    constructor(entities: Collection<Entity>) : this(entities.toList())

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "OVERRIDE_DEPRECATION")
    override fun <T> toArray(generator: IntFunction<Array<out T>>): Array<T> {
        return (delegate as java.util.Collection<*>).toArray(generator)
    }
}

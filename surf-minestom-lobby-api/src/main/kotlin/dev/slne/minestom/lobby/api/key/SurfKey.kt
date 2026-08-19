package dev.slne.minestom.lobby.api.key

import net.kyori.adventure.key.Key
import net.kyori.adventure.key.KeyPattern

/**
 * A [Key] in this server's own namespace.
 */
class SurfKey private constructor(private val key: Key) : Key by key {
    companion object {
        @KeyPattern.Namespace
        const val NAMESPACE = "surf"

        fun of(key: Key) = SurfKey(key)
        fun key(@KeyPattern.Value name: String) = SurfKey(Key.key(NAMESPACE, name))
    }

    override fun asMinimalString(): String {
        return key.asMinimalString()
    }

    override fun compareTo(that: Key): Int {
        return key.compareTo(that)
    }

    override fun key(): Key {
        return key.key()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Key) return false
        return namespace() == other.namespace() && value() == other.value()
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }

    override fun toString(): String {
        return key.toString()
    }
}

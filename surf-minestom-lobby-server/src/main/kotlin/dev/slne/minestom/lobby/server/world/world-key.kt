package dev.slne.minestom.lobby.server.world

import dev.slne.minestom.lobby.server.key.SurfKey
import net.kyori.adventure.key.Key
import net.minestom.server.instance.Instance
import net.minestom.server.tag.Tag

/**
 * The key of the stored world an instance was loaded from.
 *
 * Plugins read it from the instance tag `surf_lobby_world_key`, so the tag name must not change.
 */
private val WORLD_KEY = Tag.String("surf_lobby_world_key")

val Instance.worldKey: SurfKey?
    get() = getTag(WORLD_KEY)?.let { SurfKey.of(Key.key(it)) }

fun Instance.setWorldKey(worldKey: SurfKey) {
    setTag(WORLD_KEY, worldKey.asString())
}

package dev.slne.minestom.lobby.server.world

import net.kyori.adventure.key.Key
import net.minestom.server.instance.Instance
import net.minestom.server.tag.Tag

/**
 * The key of the stored world an instance was loaded from.
 *
 * Bundled plugins read this tag themselves and parse its value with `Key.key`, so its name and the
 * `surf:<name>` value format must not change.
 */
private val WORLD_KEY = Tag.String("surf_lobby_world_key")

val Instance.worldKey: SurfKey?
    get() = getTag(WORLD_KEY)?.let { SurfKey.of(Key.key(it)) }

internal fun Instance.setWorldKey(worldKey: SurfKey) {
    setTag(WORLD_KEY, worldKey.asString())
}

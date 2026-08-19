package dev.slne.minestom.lobby.api.instance

import dev.slne.minestom.lobby.api.key.SurfKey
import dev.slne.minestom.lobby.api.util.InternalMinestomLobbyApi
import net.kyori.adventure.key.Key
import net.minestom.server.instance.Instance
import net.minestom.server.tag.Tag

private val WORLD_KEY = Tag.String("surf_lobby_world_key")

val Instance.worldKey: SurfKey?
    get() = getTag(WORLD_KEY)?.let { SurfKey.of(Key.key(it)) }

@InternalMinestomLobbyApi
fun Instance.setWorldKey(worldKey: SurfKey) {
    setTag(WORLD_KEY, worldKey.asString())
}

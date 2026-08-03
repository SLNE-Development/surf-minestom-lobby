package dev.slne.minestom.lobby.api.plugin

interface MinestomPluginEntrypoint {
    suspend fun start()

    suspend fun stop() = Unit
}
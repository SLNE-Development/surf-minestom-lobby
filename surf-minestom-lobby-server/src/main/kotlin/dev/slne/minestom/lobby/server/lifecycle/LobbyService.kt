package dev.slne.minestom.lobby.server.lifecycle

interface LobbyService {

    val serviceName: String get() = javaClass.simpleName

    suspend fun start()

    suspend fun stop() = Unit
}

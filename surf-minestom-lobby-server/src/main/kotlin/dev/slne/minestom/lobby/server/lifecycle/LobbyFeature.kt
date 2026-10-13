package dev.slne.minestom.lobby.server.lifecycle

import dev.slne.surf.api.minestom.server.SurfMinestomFeature
import dev.slne.surf.api.minestom.server.SurfMinestomServer
import kotlinx.coroutines.runBlocking

/**
 * Runs the lobby's own services as part of the surf api.
 *
 * Installed after the features the services rely on (LuckPerms in particular) and before the
 * plugins, so the services start once LuckPerms is loaded and the lobby world exists by the time a
 * plugin is loaded. They stop after the plugins have been disabled.
 */
class LobbyFeature(private val serverLifecycle: ServerLifecycle) : SurfMinestomFeature {

    override val id: String = ID

    override fun load(api: SurfMinestomServer) = runBlocking {
        serverLifecycle.start()
    }

    override fun disable(api: SurfMinestomServer) = runBlocking {
        serverLifecycle.stop()
    }

    companion object {
        const val ID = "surf-minestom-lobby"
    }
}

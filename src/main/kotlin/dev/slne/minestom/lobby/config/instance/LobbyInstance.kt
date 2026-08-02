package dev.slne.minestom.lobby.config.instance

import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.InstanceManager
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.anvil.AnvilLoader
import net.minestom.server.world.DimensionType
import kotlin.io.path.Path

object LobbyInstance {

    fun create(instanceManager: InstanceManager): InstanceContainer {
        val lobbyPath = Path("worlds/lobby")
        val lobbyInstance = instanceManager.createInstanceContainer(
            AnvilLoader(
                lobbyPath,
                DimensionType.OVERWORLD.key()
            )
        )

        lobbyInstance.setChunkSupplier(::LightingChunk)

        return lobbyInstance
    }
}
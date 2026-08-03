package dev.slne.minestom.lobby.server.world

import com.google.inject.Inject
import dev.slne.minestom.lobby.server.config.ServerConfig
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.InstanceManager
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.anvil.AnvilLoader
import net.minestom.server.world.DimensionType
import kotlin.io.path.Path

class LobbyWorldFactory @Inject constructor(
    private val instanceManager: InstanceManager,
    private val config: ServerConfig,
) {

    fun create(): InstanceContainer {
        val instance = instanceManager.createInstanceContainer(
            AnvilLoader(
                Path("worlds/lobby"),
                DimensionType.OVERWORLD.key(),
            )
        )

        instance.setChunkSupplier(::LightingChunk)

        return instance
    }
}
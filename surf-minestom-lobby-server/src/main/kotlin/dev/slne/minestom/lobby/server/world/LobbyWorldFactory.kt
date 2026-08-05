package dev.slne.minestom.lobby.server.world

import com.google.inject.Inject
import dev.slne.minestom.lobby.api.extension.buildInstance
import dev.slne.minestom.lobby.server.config.ServerConfig
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.InstanceManager
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.anvil.AnvilLoader
import net.minestom.server.world.DimensionType
import kotlin.io.path.Path

class LobbyWorldFactory @Inject constructor(
    private val config: ServerConfig,
) {

    fun create(): InstanceContainer = buildInstance {
        chunkLoader = AnvilLoader(
            Path("worlds/lobby"),
            DimensionType.OVERWORLD.key(),
        )

        setChunkSupplier(::LightingChunk)
    }

}
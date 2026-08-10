package dev.slne.minestom.lobby.server.world

import com.google.inject.Inject
import dev.slne.minestom.lobby.api.extension.buildInstance
import dev.slne.minestom.lobby.api.extension.generator
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.world.block.LobbyBlockHandlers
import dev.slne.minestom.lobby.server.world.entity.AnvilEntitySource
import dev.slne.minestom.lobby.server.world.entity.VanillaEntityImporter
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.anvil.AnvilLoader
import net.minestom.server.instance.block.Block
import net.minestom.server.world.DimensionType
import kotlin.io.path.Path
import kotlin.io.path.exists

class LobbyWorldFactory @Inject constructor(
    private val config: ServerConfig,
) {
    companion object {
        private val LOGGER = ComponentLogger.logger()
    }

    suspend fun create(): InstanceContainer {
        val path = Path("worlds/lobby")

        val instance = buildInstance {
            if (path.exists()) {
                LobbyBlockHandlers.register()
                chunkLoader = AnvilLoader(path, DimensionType.OVERWORLD.key())
            } else {
                LOGGER.warn(
                    "Missing lobby world at '{}' creating a non-persistent flat world...",
                    path
                )
                generator {
                    modifier().fillHeight(0, 1, Block.BEDROCK)
                    modifier().fillHeight(1, 64, Block.DIRT)
                    modifier().fillHeight(64, 65, Block.GRASS_BLOCK)
                }
            }

            setChunkSupplier(::LightingChunk)
        }

        if (path.exists()) {
            VanillaEntityImporter.importInto(
                instance,
                AnvilEntitySource(path, DimensionType.OVERWORLD.key())
            )
        }

        return instance
    }
}
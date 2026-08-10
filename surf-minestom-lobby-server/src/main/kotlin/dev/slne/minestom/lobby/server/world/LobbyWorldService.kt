package dev.slne.minestom.lobby.server.world

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.extension.buildInstance
import dev.slne.minestom.lobby.server.lifecycle.LobbyService
import dev.slne.minestom.lobby.server.world.block.LobbyBlockHandlers
import dev.slne.minestom.lobby.server.world.entity.AnvilEntitySource
import dev.slne.minestom.lobby.server.world.entity.VanillaEntityImporter
import dev.slne.minestom.lobby.server.world.generator.FlatWorldGenerator
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.anvil.AnvilLoader
import net.minestom.server.world.DimensionType
import kotlin.io.path.Path
import kotlin.io.path.exists

@Singleton
class LobbyWorldService @Inject constructor() : LobbyService {

    private var container: InstanceContainer? = null

    val instance: InstanceContainer
        get() = checkNotNull(container) {
            "The lobby world has not been created yet - LobbyWorldService.start() has to run first"
        }

    override suspend fun start() {
        val persistent = WORLD_PATH.exists()

        if (persistent) {
            LobbyBlockHandlers.register()
        } else {
            LOGGER.warn(
                "Missing lobby world at '{}', creating a non-persistent flat world instead.",
                WORLD_PATH
            )
        }

        val world = buildInstance {
            if (persistent) {
                chunkLoader = AnvilLoader(WORLD_PATH, DimensionType.OVERWORLD.key())
            } else {
                setGenerator(FlatWorldGenerator)
            }

            setChunkSupplier(::LightingChunk)
        }

        container = world

        if (persistent) {
            VanillaEntityImporter.importInto(
                world,
                AnvilEntitySource(WORLD_PATH, DimensionType.OVERWORLD.key())
            )
        }
    }

    private companion object {
        val LOGGER = ComponentLogger.logger()
        val WORLD_PATH = Path("worlds/lobby")
    }
}

package dev.slne.minestom.lobby.server.world

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.extension.DimensionTypeRegistry
import dev.slne.minestom.lobby.api.extension.buildInstance
import dev.slne.minestom.lobby.api.instance.setWorldKey
import dev.slne.minestom.lobby.api.key.SurfKey
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.database.world.LobbyWorldRepository
import dev.slne.minestom.lobby.server.lifecycle.LobbyService
import dev.slne.minestom.lobby.server.world.block.LobbyBlockHandlers
import dev.slne.minestom.lobby.server.world.entity.PolarPaperWorldAccess
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.future.await
import net.hollowcube.polar.PolarLoader
import net.hollowcube.polar.PolarReader
import net.minestom.server.MinecraftServer
import net.minestom.server.instance.Chunk
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.LightingChunk
import net.minestom.server.world.DimensionType
import java.util.concurrent.CompletableFuture

@Singleton
class LobbyWorldService @Inject constructor(
    private val repository: LobbyWorldRepository,
    private val config: ServerConfig,
) : LobbyService {

    private var container: InstanceContainer? = null

    val instance: InstanceContainer
        get() = checkNotNull(container) {
            "The lobby world has not been created yet - LobbyWorldService.start() has to run first"
        }

    override suspend fun start() {
        val storedWorld = checkNotNull(repository.find(config.world.databaseKey)) {
            "Polar world '${config.world.databaseKey}' does not exist in the database. Place " +
                    "'${config.world.databaseKey}.polar' into 'upload/worlds' and start again."
        }

        val overworld = checkNotNull(DimensionTypeRegistry.get(DimensionType.OVERWORLD))
        val spaceDimension = DimensionTypeRegistry.register(
            SurfKey.key("space_lobby"),
            buildSpaceDimensionType(overworld),
        )

        container = buildInstance(spaceDimension) {
            chunkLoader = PolarLoader(PolarReader.read(storedWorld.data))
                .setWorldAccess(PolarPaperWorldAccess())

            setChunkSupplier(::LightingChunk)
            setWorldKey(storedWorld.surfKey())
        }

        LobbyBlockHandlers.register()

        val forceLoad = config.forceLoad
        if (forceLoad.enabled) {
            forceLoadChunks(instance, forceLoad)
        }
    }

    private suspend fun forceLoadChunks(
        instance: InstanceContainer,
        config: ServerConfig.ForceLoadConfig,
    ) {
        val minChunkX = minOf(config.from.x, config.to.x)
        val maxChunkX = maxOf(config.from.x, config.to.x)
        val minChunkZ = minOf(config.from.z, config.to.z)
        val maxChunkZ = maxOf(config.from.z, config.to.z)

        val chunkCount = (maxChunkX - minChunkX + 1L) * (maxChunkZ - minChunkZ + 1L)

        MinecraftServer.LOGGER.info(
            "Force-loading {} chunks from {} to {}.",
            chunkCount,
            config.from,
            config.to,
        )

        val loads = ObjectArrayList<CompletableFuture<Chunk>>(chunkCount.toInt())

        for (chunkX in minChunkX..maxChunkX) {
            for (chunkZ in minChunkZ..maxChunkZ) {
                loads += instance.loadChunk(chunkX, chunkZ)
            }
        }

        CompletableFuture
            .allOf(*loads.toTypedArray())
            .await()
    }
}

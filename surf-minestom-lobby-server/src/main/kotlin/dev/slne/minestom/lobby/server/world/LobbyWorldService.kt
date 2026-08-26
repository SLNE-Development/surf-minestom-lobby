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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import net.hollowcube.polar.PolarLoader
import net.hollowcube.polar.PolarReader
import net.minestom.server.MinecraftServer
import net.minestom.server.ServerFlag
import net.minestom.server.instance.Chunk
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.LightingChunk
import net.minestom.server.world.DimensionType
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

@Singleton
class LobbyWorldService @Inject constructor(
    private val repository: LobbyWorldRepository,
    private val config: ServerConfig,
) : LobbyService {

    private var container: InstanceContainer? = null

    @Volatile
    private var requiredChunks: ChunkRegion? = null

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
            val required = forceLoad.requiredChunks(instance.viewDistance())
            requiredChunks = required

            forceLoadChunks(instance, forceLoad.chunkRegion(), required)
        }
    }

    suspend fun warmChunkPackets() {
        val required = requiredChunks ?: return
        val instance = this.instance

        if (!ServerFlag.CACHED_PACKET) {
            MinecraftServer.LOGGER.info("Packet caching is disabled, skipping the chunk packet warmup.")
            return
        }

        val chunks = ObjectArrayList<Chunk>(required.chunkCount.toInt())
        required.forEach { chunkX, chunkZ ->
            instance.getChunk(chunkX, chunkZ)?.let(chunks::add)
        }

        MinecraftServer.LOGGER.info("Warming the chunk packets of {} chunks.", chunks.size)

        val startedAt = System.nanoTime()

        val warmed = withContext(Dispatchers.Default) {
            chunks
                .map { chunk -> async { chunk.warmFullDataPacket() } }
                .awaitAll()
        }.count { it }

        val duration = (System.nanoTime() - startedAt).nanoseconds.inWholeMilliseconds.milliseconds

        MinecraftServer.LOGGER.info(
            "Warmed the chunk packets of {} chunks in {}.",
            warmed,
            duration,
        )

        if (warmed.toLong() != required.chunkCount) {
            MinecraftServer.LOGGER.warn(
                "Only {} of the {} chunks {} are warm; the rest are not loaded.",
                warmed,
                required.chunkCount,
                required,
            )
        }
    }

    private suspend fun forceLoadChunks(
        instance: InstanceContainer,
        playableArea: ChunkRegion,
        required: ChunkRegion,
    ) {
        MinecraftServer.LOGGER.info(
            "Force-loading {} chunks {} - the playable area {} plus {} chunks of view distance.",
            required.chunkCount,
            required,
            playableArea,
            instance.viewDistance() + 1,
        )

        val loads = ObjectArrayList<CompletableFuture<Chunk>>(required.chunkCount.toInt())

        required.forEach { chunkX, chunkZ ->
            loads += instance.loadChunk(chunkX, chunkZ)
        }

        CompletableFuture
            .allOf(*loads.toTypedArray())
            .await()
    }
}

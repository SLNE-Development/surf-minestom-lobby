package dev.slne.minestom.lobby.server.world

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.extension.buildInstance
import dev.slne.minestom.lobby.api.instance.setWorldKey
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.database.world.LobbyWorldRepository
import dev.slne.minestom.lobby.server.lifecycle.LobbyService
import dev.slne.minestom.lobby.server.world.block.LobbyBlockHandlers
import dev.slne.minestom.lobby.server.world.entity.PolarPaperWorldAccess
import net.hollowcube.polar.PolarLoader
import net.hollowcube.polar.PolarReader
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.LightingChunk

@Singleton
class LobbyWorldService @Inject constructor(
    private val repository: LobbyWorldRepository,
    private val config: ServerConfig.WorldConfig,
) : LobbyService {

    private var container: InstanceContainer? = null

    val instance: InstanceContainer
        get() = checkNotNull(container) {
            "The lobby world has not been created yet - LobbyWorldService.start() has to run first"
        }

    override suspend fun start() {
        val storedWorld = checkNotNull(repository.find(config.databaseKey)) {
            "Polar world '${config.databaseKey}' does not exist in the database. Place " +
                    "'${config.databaseKey}.polar' into 'upload/worlds' and start again."
        }

        container = buildInstance {
            chunkLoader = PolarLoader(PolarReader.read(storedWorld.data))
                .setWorldAccess(PolarPaperWorldAccess())

            setChunkSupplier(::LightingChunk)
            setWorldKey(storedWorld.surfKey())
        }

        LobbyBlockHandlers.register()
    }
}

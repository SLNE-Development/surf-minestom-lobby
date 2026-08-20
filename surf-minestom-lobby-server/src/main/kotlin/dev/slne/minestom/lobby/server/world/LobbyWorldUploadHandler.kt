package dev.slne.minestom.lobby.server.world

import com.google.common.hash.Hashing
import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.key.SurfKey
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.database.world.LobbyWorldEntity
import dev.slne.minestom.lobby.server.database.world.LobbyWorldRepository
import dev.slne.minestom.lobby.server.upload.UploadEntry
import dev.slne.minestom.lobby.server.upload.UploadHandler
import net.kyori.adventure.key.InvalidKeyException
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readBytes

@Singleton
class LobbyWorldUploadHandler @Inject constructor(
    private val repository: LobbyWorldRepository,
    private val config: ServerConfig.WorldConfig,
) : UploadHandler {

    private companion object {
        val LOGGER = ComponentLogger.logger()
        const val MAX_KEY_LENGTH = 64
    }

    override val directoryName = "worlds"
    override val fileGlob = "*.polar"

    override val description =
        "Polar worlds. The lobby spawns players in the world configured as `world.database-key`."

    override val fileNameDocumentation =
        "`<world-key>.polar` - the name without the extension becomes the world key. It has to be " +
                "a valid key (`[a-z0-9_.-]`) of at most $MAX_KEY_LENGTH characters, for example " +
                "`lobby.polar`."

    override suspend fun publish(file: Path) {
        val key = requireWorldKey(file.nameWithoutExtension)

        val data = file.readBytes()
        val sha256 = Hashing.sha256()
            .hashBytes(data)
            .toString()

        if (repository.findSha256(key) == sha256) {
            LOGGER.info("Polar world '{}' is already up to date.", key)
            return
        }

        repository.upsert(
            LobbyWorldEntity(
                key = key,
                sha256 = sha256,
                data = data,
            )
        )

        LOGGER.info("Published Polar world '{}' from '{}'.", key, file)
    }

    override suspend fun list(): List<UploadEntry> = repository.findAllKeys().map { key ->
        UploadEntry(
            key = key,
            detail = "the world this lobby loads".takeIf { key == config.databaseKey },
        )
    }

    override suspend fun delete(key: String): Boolean {
        check(key != config.databaseKey) {
            "'$key' is the world this lobby loads (world.database-key), so deleting it would " +
                    "break the next start"
        }

        return repository.delete(key)
    }

    private fun requireWorldKey(key: String): String {
        require(key.isNotBlank()) { "Polar world file has an empty world key" }
        require(key.length <= MAX_KEY_LENGTH) {
            "Polar world key '$key' exceeds the maximum length of $MAX_KEY_LENGTH characters"
        }

        try {
            SurfKey.key(key)
        } catch (invalidKey: InvalidKeyException) {
            throw IllegalArgumentException(
                "Polar world key '$key' is not a valid key: ${invalidKey.message}",
                invalidKey
            )
        }

        return key
    }
}

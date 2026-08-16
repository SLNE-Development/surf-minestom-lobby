package dev.slne.minestom.lobby.server.world

import com.google.common.hash.Hashing
import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.database.world.LobbyWorldEntity
import dev.slne.minestom.lobby.server.database.world.LobbyWorldRepository
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.nio.file.Path
import kotlin.io.path.*

@Singleton
class LobbyWorldPublisher @Inject constructor(
    private val repository: LobbyWorldRepository,
) {
    private companion object {
        val LOGGER = ComponentLogger.logger()
        val WORLD_DIRECTORY = Path("world")
    }

    suspend fun publishAll() {
        if (!WORLD_DIRECTORY.exists()) {
            WORLD_DIRECTORY.createDirectories()
            LOGGER.info(
                "Created world directory at '{}'. Place a polar world into it to load it.",
                WORLD_DIRECTORY
            )
            return
        }

        WORLD_DIRECTORY
            .listDirectoryEntries("*.polar")
            .forEach { publish(it) }
    }

    @Suppress("UnstableApiUsage")
    private suspend fun publish(file: Path) {
        val key = file.nameWithoutExtension

        require(key.isNotBlank()) { "Polar world file has an empty database key: $file" }
        require(key.length <= 64) { "Polar world key '$key' exceeds the maximum length of 64 characters" }

        val data = file.readBytes()
        val sha256 = Hashing.sha256()
            .hashBytes(data)
            .toString()

        val existingSha256 = repository.findSha256(key)

        if (existingSha256 == sha256) {
            LOGGER.info(
                "Polar world '{}' is already up to date, removing publish file '{}'.",
                key,
                file
            )

            file.deleteIfExists()
            return
        }

        LOGGER.info("Publishing Polar world '{}' from '{}'.", key, file)

        repository.upsert(
            LobbyWorldEntity(
                key = key,
                sha256 = sha256,
                data = data,
            )
        )

        file.deleteIfExists()
        LOGGER.info("Published Polar world '{}'.", key)
    }
}
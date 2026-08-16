package dev.slne.minestom.lobby.server.world.entity

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.hollowcube.polar.PolarWorldAccess
import net.kyori.adventure.nbt.BinaryTagIO
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Chunk
import net.minestom.server.network.NetworkBuffer
import net.minestom.server.network.NetworkBuffer.*

class PolarPaperWorldAccess : PolarWorldAccess {
    private companion object {
        val LOGGER = ComponentLogger.logger()

        const val CHUNK_SIZE = 16.0

        const val ENTITIES_VERSION = 1
        const val PERSISTENT_DATA_CONTAINER_VERSION = 2
        const val CURRENT_VERSION = 2
    }

    private data class StoredEntity(
        val nbt: CompoundBinaryTag,
        val position: Pos
    )

    override fun loadChunkData(chunk: Chunk, userData: NetworkBuffer?) {
        if (userData == null) return

        val version = userData.read(BYTE)
        require(version in ENTITIES_VERSION..CURRENT_VERSION) { "Unsupported PolarPaper chunk userdata version: $version" }

        val entityCount = userData.read(VAR_INT)
        require(entityCount >= 0) { "Invalid PolarPaper entity count: $entityCount" }

        val entities = ObjectArrayList<StoredEntity>(entityCount)

        repeat(entityCount) {
            val localX = userData.read(DOUBLE)
            val y = userData.read(DOUBLE)
            val localZ = userData.read(DOUBLE)
            val yaw = userData.read(FLOAT)
            val pitch = userData.read(FLOAT)

            val nbtBytes = userData.read(BYTE_ARRAY)

            val x = (chunk.chunkX shl 4) + normalizeLocalCoordinate(localX)
            val z = (chunk.chunkZ shl 4) + normalizeLocalCoordinate(localZ)

            val position = Pos(x, y, z, yaw, pitch)

            val nbt = try {
                readEntityNbt(nbtBytes)
            } catch (failure: Throwable) {
                LOGGER.warn(
                    "Failed to decode PolarPaper entity in chunk {}, {}",
                    chunk.chunkX,
                    chunk.chunkZ,
                    failure,
                )

                return@repeat
            }

            entities += StoredEntity(
                nbt = nbt,
                position = position,
            )
        }

        // Read PersistentDataContainer
        if (version >= PERSISTENT_DATA_CONTAINER_VERSION) {
            userData.read(BYTE_ARRAY)
        }

        if (entities.isEmpty) return

        chunk.instance.scheduleNextTick { instance ->
            for ((nbt, position) in entities) {
                VanillaEntityImporter.spawn(
                    instance = instance,
                    nbt = nbt,
                    position = position,
                )
            }
        }
    }

    private fun readEntityNbt(
        data: ByteArray
    ): CompoundBinaryTag = data.inputStream().use { input ->
        BinaryTagIO
            .reader()
            .read(input, BinaryTagIO.Compression.NONE)
    }

    private fun normalizeLocalCoordinate(
        coordinate: Double,
    ): Double = if (coordinate < 0.0) {
        coordinate + CHUNK_SIZE
    } else {
        coordinate
    }
}
package dev.slne.minestom.lobby.server.world.entity

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.BinaryTagTypes
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.minestom.server.instance.anvil.AnvilRegionAccess
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.path.name

/**
 * Reads the entity region files of an Anvil world.
 */
class AnvilEntitySource(worldPath: Path, dimension: Key) {

    private val entitiesPath = worldPath
        .resolve("dimensions")
        .resolve(dimension.namespace())
        .resolve(dimension.value())
        .resolve("entities")

    val exists get() = entitiesPath.exists()

    /**
     * Reads all entities in the world at once. Because this is only a small lobby
     * world, reading everything in one go is fine.
     */
    fun readAll(): List<CompoundBinaryTag> {
        if (!exists) return emptyList()

        val entities = ObjectArrayList<CompoundBinaryTag>()

        entitiesPath.forEachDirectoryEntry("r.*.mca") { file ->
            val region = file.regionCoordinates() ?: return@forEachDirectoryEntry

            AnvilRegionAccess.open(file).use { access ->
                for (localX in 0 until REGION_SIZE) {
                    for (localZ in 0 until REGION_SIZE) {
                        val chunk = access.readChunk(
                            region.first * REGION_SIZE + localX,
                            region.second * REGION_SIZE + localZ
                        ) ?: continue

                        for (tag in chunk.getList("Entities", BinaryTagTypes.COMPOUND)) {
                            entities += tag as CompoundBinaryTag
                        }
                    }
                }
            }
        }

        return entities
    }

    /**
     * Extracts the region coordinates from a region file path.
     */
    private fun Path.regionCoordinates(): Pair<Int, Int>? {
        val parts = name.removePrefix("r.").removeSuffix(".mca").split('.')
        if (parts.size != 2) return null

        val x = parts[0].toIntOrNull() ?: return null
        val z = parts[1].toIntOrNull() ?: return null

        return x to z
    }

    private companion object {
        const val REGION_SIZE = 32
    }
}

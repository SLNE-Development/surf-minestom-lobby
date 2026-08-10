package dev.slne.minestom.lobby.server.world.entity

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntMaps
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.asDeferred
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.MetadataHolder
import net.minestom.server.entity.metadata.LivingEntityMeta
import net.minestom.server.instance.Instance
import java.util.concurrent.ConcurrentHashMap

object VanillaEntityImporter {

    private val LOGGER = ComponentLogger.logger()

    suspend fun importInto(instance: Instance, source: AnvilEntitySource): Summary {
        if (!source.exists) {
            LOGGER.info("World has no entities directory, skipping entity import")
            return Summary.EMPTY
        }

        val spawned = Object2IntLinkedOpenHashMap<String>()
        val failed = Object2IntLinkedOpenHashMap<String>()
        val pending = ObjectArrayList<Deferred<*>>()

        val lock = Any()

        for (nbt in source.readAll()) {
            val id = nbt.getString("id").ifEmpty { "<no id>" }

            try {
                val entity = build(nbt)

                if (entity == null) {
                    synchronized(lock) {
                        failed.addTo(id, 1)
                    }
                    continue
                }

                pending += entity.setInstance(instance, nbt.posOrNull()!!)
                    .handle { _, spawnFailure ->
                        synchronized(lock) {
                            if (spawnFailure == null) {
                                spawned.addTo(id, 1)
                            } else {
                                failed.addTo(id, 1)
                            }
                        }
                        spawnFailure?.let { LOGGER.debug("Failed to spawn a {}", id, it) }
                    }.asDeferred()
            } catch (failure: Throwable) {
                synchronized(lock) {
                    failed.addTo(id, 1)
                }
                LOGGER.debug("Failed to import a {}", id, failure)
            }
        }

        pending.awaitAll()
        return Summary(spawned, failed).also { it.log() }
    }

    fun build(nbt: CompoundBinaryTag): Entity? {
        val type = EntityType.fromKey(nbt.getString("id")) ?: return null
        if (nbt.posOrNull() == null) return null

        val uuid = nbt.uuidOrNull()

        val entity = when {
            type.isLiving() -> if (uuid != null) LivingEntity(type, uuid) else LivingEntity(type)
            uuid != null -> Entity(type, uuid)
            else -> Entity(type)
        }

        entity.applyCommon(nbt)
        EntityNbtMappers[type]?.apply(entity, nbt)

        return entity
    }

    private val LIVING_TYPES = ConcurrentHashMap<EntityType, Boolean>()

    @Suppress("UnstableApiUsage")
    private fun EntityType.isLiving(): Boolean = LIVING_TYPES.computeIfAbsent(this) {
        MetadataHolder.createMeta(it, null, MetadataHolder { }) is LivingEntityMeta
    }

    data class Summary(
        val spawned: Object2IntMap<String>,
        val failed: Object2IntMap<String>
    ) {
        val spawnedCount get() = spawned.values.sum()
        val failedCount get() = failed.values.sum()

        fun log() {
            if (spawnedCount == 0 && failedCount == 0) {
                LOGGER.info("No entities found in the world's entities directory")
                return
            }

            LOGGER.info("Imported {} entities: {}", spawnedCount, spawned.describe())

            if (failedCount > 0) {
                LOGGER.warn(
                    "Skipped {} entities that could not be imported: {}. Run with debug logging for the causes.",
                    failedCount,
                    failed.describe()
                )
            }
        }

        private fun Map<String, Int>.describe() = entries
            .sortedByDescending { it.value }
            .joinToString { "${it.value}x ${it.key}" }

        companion object {
            val EMPTY = Summary(Object2IntMaps.emptyMap(), Object2IntMaps.emptyMap())
        }
    }
}

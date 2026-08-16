package dev.slne.minestom.lobby.server.world.entity

import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.MetadataHolder
import net.minestom.server.entity.metadata.LivingEntityMeta
import net.minestom.server.instance.Instance
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object VanillaEntityImporter {

    private val LOGGER = ComponentLogger.logger()

    fun spawn(
        instance: Instance,
        nbt: CompoundBinaryTag,
        position: Pos,
    ): CompletableFuture<Void>? {
        val id = nbt
            .getString("id")
            .ifEmpty { "<no id>" }

        val entity = try {
            build(nbt)
        } catch (failure: Throwable) {
            LOGGER.debug("Failed to build entity '{}'", id, failure)
            return null
        }

        if (entity == null) {
            LOGGER.debug("Skipping unsupported entity '{}'", id)
            return null
        }

        return entity
            .setInstance(instance, position)
            .whenComplete { _, failure ->
                if (failure != null) {
                    LOGGER.debug("Failed to spawn entity '{}'", id, failure)
                }
            }
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
}

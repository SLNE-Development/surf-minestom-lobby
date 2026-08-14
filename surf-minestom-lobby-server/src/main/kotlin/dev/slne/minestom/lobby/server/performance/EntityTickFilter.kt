package dev.slne.minestom.lobby.server.performance

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectSets
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType

object EntityTickFilter {

    @Volatile
    private var disabledTypes: Set<EntityType> = emptySet()

    @JvmStatic
    fun shouldTick(entity: Entity): Boolean =
        entity.entityType !in disabledTypes

    fun configure(types: Set<EntityType>) {
        disabledTypes = ObjectSets.unmodifiable(ObjectOpenHashSet(types))
    }
}
package dev.slne.minestom.lobby.server.performance

import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType

object EntityTickFilter {

    @Volatile
    private var disabledTypeIds = BooleanArray(0)

    /**
     * Determines whether the specified entity should proceed with its tick cycle.
     *
     * @param entity The entity to evaluate for ticking. This entity's type is used
     *               to check against a list of disabled entity types.
     * @return True if the entity is allowed to tick; false if the entity's type is
     *         disabled or out of the valid index range.
     */
    @JvmStatic
    fun shouldTick(entity: Entity): Boolean {
        val disabled = disabledTypeIds
        val id = entity.entityType.id()
        return id < 0 || id >= disabled.size || !disabled[id]
    }

    /**
     * Configures the internal disabled entity types based on the given set of entity types.
     * Each entity type in the set will be marked as disabled.
     *
     * @param types the set of entity types to be marked as disabled. Each entity type must have a unique ID.
     */
    fun configure(types: Set<EntityType>) {
        val highestId = types.maxOfOrNull(EntityType::id) ?: -1
        val disabled = BooleanArray(highestId + 1)

        types.forEach { type ->
            disabled[type.id()] = true
        }

        disabledTypeIds = disabled
    }
}

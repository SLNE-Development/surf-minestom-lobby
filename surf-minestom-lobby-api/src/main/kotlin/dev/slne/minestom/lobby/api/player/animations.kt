package dev.slne.minestom.lobby.api.player

import dev.slne.minestom.lobby.api.command.entity.editEntityMeta
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityPose
import net.minestom.server.entity.metadata.LivingEntityMeta
import net.minestom.server.timer.TaskSchedule

/**
 * Plays the riptide spin animation on this entity for [durationTicks] ticks.
 */
fun Entity.playSpinAttackAnimation(durationTicks: Int = 20) {
    if (entityMeta !is LivingEntityMeta) return

    editEntityMeta<LivingEntityMeta> { meta ->
        meta.isInRiptideSpinAttack = true
        pose = EntityPose.SPIN_ATTACK
    }

    scheduler().buildTask {
        editEntityMeta<LivingEntityMeta> { meta ->
            meta.isInRiptideSpinAttack = false

            if (pose == EntityPose.SPIN_ATTACK) {
                pose = EntityPose.STANDING
            }
        }
    }.delay(TaskSchedule.tick(durationTicks)).schedule()
}

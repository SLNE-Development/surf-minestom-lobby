package dev.slne.minestom.lobby.api.player

import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityPose
import net.minestom.server.timer.TaskSchedule

/**
 * Plays the riptide spin animation on this entity for [durationTicks] ticks.
 *
 * The animation is pure client rendering through the entity's pose; afterwards the pose is
 * reset to standing, unless something else changed it in the meantime.
 */
fun Entity.playSpinAttackAnimation(durationTicks: Int = 20) {
    pose = EntityPose.SPIN_ATTACK

    scheduler().buildTask {
        if (pose == EntityPose.SPIN_ATTACK) {
            pose = EntityPose.STANDING
        }
    }.delay(TaskSchedule.tick(durationTicks)).schedule()
}

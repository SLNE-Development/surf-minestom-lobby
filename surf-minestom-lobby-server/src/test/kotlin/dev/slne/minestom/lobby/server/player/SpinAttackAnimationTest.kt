package dev.slne.minestom.lobby.server.player

import dev.slne.minestom.lobby.api.player.playSpinAttackAnimation
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityPose
import net.minestom.server.entity.EntityType
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EnvTest
class SpinAttackAnimationTest {

    @Test
    fun `spin attack animation sets the pose and resets it afterwards`(env: Env) {
        val entity = spawnEntity(env)

        entity.playSpinAttackAnimation(durationTicks = 2)
        assertEquals(EntityPose.SPIN_ATTACK, entity.pose)

        repeat(3) { env.tick() }

        assertEquals(EntityPose.STANDING, entity.pose)
    }

    @Test
    fun `spin attack animation keeps a pose that changed in the meantime`(env: Env) {
        val entity = spawnEntity(env)

        entity.playSpinAttackAnimation(durationTicks = 2)
        entity.pose = EntityPose.SNEAKING

        repeat(3) { env.tick() }

        assertEquals(EntityPose.SNEAKING, entity.pose)
    }

    private fun spawnEntity(env: Env): Entity {
        val instance = env.createFlatInstance()
        instance.loadChunk(0, 0).join()

        val entity = Entity(EntityType.ZOMBIE)
        entity.setInstance(instance, Pos(0.5, 42.0, 0.5)).join()
        return entity
    }
}

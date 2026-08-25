package dev.slne.minestom.lobby.server.npc

import codes.bed.minestom.npc.StomNPCs
import dev.slne.minestom.lobby.server.mixin.MixinTransformingClassLoader
import java.lang.reflect.Method
import java.util.concurrent.CopyOnWriteArrayList
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityTeleportEvent
import net.minestom.server.instance.Instance
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Drives the mixin-transformed StomNPCs controllers against a real instance and counts the
 * [EntityTeleportEvent]s their auxiliary entities emit, so the suppressed per-tick teleport is
 * observed rather than inferred.
 *
 * The transformed controllers are loaded through a child class loader because the production agent
 * is not installed under `gradlew test`; everything outside `codes.bed.minestom.npc.display` still
 * resolves to the ordinary classes.
 */
@EnvTest
class NpcDisplaySyncTest {

    // StomNPCs is owned too so this test gets a private NpcManager instead of racing the
    // process-wide one other npc tests initialize.
    private val loader = MixinTransformingClassLoader(
        javaClass.classLoader,
        "codes.bed.minestom.npc.display.",
        "codes.bed.minestom.npc.StomNPCs",
    )

    private val teleports = CopyOnWriteArrayList<Entity>()
    private val failures = CopyOnWriteArrayList<Throwable>()

    @Test
    fun `a static npc stops teleporting its text display`(env: Env) {
        val world = world(env)
        val display = world.textDisplay()

        world.tick(20)

        assertEquals(0, teleports.count(display.entity()!!), "the hologram kept teleporting")
        assertEquals(world.npc.position.add(TEXT_OFFSET), display.entity()!!.position)
    }

    @Test
    fun `a static npc stops teleporting its interaction hitbox`(env: Env) {
        val world = world(env)
        val interaction = world.interaction()

        world.tick(20)

        assertEquals(0, teleports.count(interaction.entity()!!), "the hitbox kept teleporting")
        assertEquals(world.npc.position.add(INTERACTION_OFFSET), interaction.entity()!!.position)
    }

    @Test
    fun `moving the npc synchronizes both auxiliary entities exactly once`(env: Env) {
        val world = world(env)
        val display = world.textDisplay()
        val interaction = world.interaction()
        world.tick(5)
        teleports.clear()

        val moved = Pos(8.5, 43.0, -4.5)
        world.npc.teleport(moved).join()
        world.tick(1)

        assertEquals(1, teleports.count(display.entity()!!), "the hologram did not follow")
        assertEquals(1, teleports.count(interaction.entity()!!), "the hitbox did not follow")
        assertEquals(moved.add(TEXT_OFFSET), display.entity()!!.position)
        assertEquals(moved.add(INTERACTION_OFFSET), interaction.entity()!!.position)

        teleports.clear()
        world.tick(10)
        assertEquals(0, teleports.count(display.entity()!!), "the hologram never settled")
        assertEquals(0, teleports.count(interaction.entity()!!), "the hitbox never settled")
    }

    @Test
    fun `changing npc yaw and pitch stays equivalent to the original library`(env: Env) {
        val world = world(env)
        val display = world.textDisplay()
        val interaction = world.interaction()
        world.tick(5)
        teleports.clear()

        world.npc.setView(37.5f, -12.25f)
        // the original library teleports to exactly this position, rotation included
        val expectedDisplay = world.npc.position.add(TEXT_OFFSET)
        val expectedInteraction = world.npc.position.add(INTERACTION_OFFSET)
        world.tick(1)

        assertEquals(1, teleports.count(display.entity()!!), "the hologram ignored the rotation")
        assertEquals(1, teleports.count(interaction.entity()!!), "the hitbox ignored the rotation")
        assertEquals(expectedDisplay, display.entity()!!.position)
        assertEquals(expectedInteraction, interaction.entity()!!.position)

        teleports.clear()
        world.tick(10)
        assertEquals(0, teleports.count(display.entity()!!), "the hologram never settled")
        assertEquals(0, teleports.count(interaction.entity()!!), "the hitbox never settled")
    }

    @Test
    fun `changing the text display offset moves the display`(env: Env) {
        val world = world(env)
        val display = world.textDisplay()
        world.tick(5)
        teleports.clear()

        val raised = Vec(0.0, 3.4, 0.0)
        display.updateOffset(raised)
        world.tick(1)

        assertEquals(raised, display.offset())
        assertEquals(world.npc.position.add(raised), display.entity()!!.position)
        assertEquals(1, teleports.count(display.entity()!!), "the offset change was swallowed")

        teleports.clear()
        world.tick(10)
        assertEquals(0, teleports.count(display.entity()!!), "the new offset never settled")
    }

    @Test
    fun `the initial attach places both auxiliary entities`(env: Env) {
        val world = world(env)

        val display = world.textDisplay()
        val interaction = world.interaction()

        val displayEntity = checkNotNull(display.entity()) { "no hologram was spawned" }
        val interactionEntity = checkNotNull(interaction.entity()) { "no hitbox was spawned" }
        assertEquals(EntityType.TEXT_DISPLAY, displayEntity.entityType)
        assertEquals(EntityType.INTERACTION, interactionEntity.entityType)
        assertSame(world.instance, displayEntity.instance)
        assertSame(world.instance, interactionEntity.instance)
        assertEquals(world.npc.position.add(TEXT_OFFSET), displayEntity.position)
        assertEquals(world.npc.position.add(INTERACTION_OFFSET), interactionEntity.position)

        // attachTo already positioned them, so the very first sync has nothing left to do
        world.tick(1)
        assertEquals(0, teleports.count(displayEntity), "the hologram was re-teleported on attach")
        assertEquals(0, teleports.count(interactionEntity), "the hitbox was re-teleported on attach")
    }

    @Test
    fun `detaching leaves no broken state`(env: Env) {
        val world = world(env)
        val display = world.textDisplay()
        val interaction = world.interaction()
        world.tick(5)

        val displayEntity = display.entity()!!
        val interactionEntity = interaction.entity()!!
        display.detach()
        interaction.detach()

        assertNull(display.entity(), "the hologram handle survived detach")
        assertNull(interaction.entity(), "the hitbox handle survived detach")
        assertTrue(displayEntity.isRemoved, "the hologram entity was not removed")
        assertTrue(interactionEntity.isRemoved, "the hitbox entity was not removed")

        teleports.clear()
        world.tick(10)

        assertEquals(0, teleports.size, "a detached controller still teleported something")
        assertTrue(failures.isEmpty(), "ticking a detached controller logged $failures")
    }

    private fun world(env: Env): NpcWorld {
        val node = EventNode.type("npc-display-sync", EventFilter.INSTANCE)
        env.process().eventHandler().addChild(node)
        loader.loadClass(StomNPCs::class.java.name)
            .getMethod("initialize", EventNode::class.java)
            .invoke(null, node)

        env.process().exception().setExceptionHandler { failures += it }
        env.process().eventHandler().addListener(EntityTeleportEvent::class.java) {
            teleports += it.entity
        }

        val instance = env.createFlatInstance()
        instance.loadChunk(0, 0).join()
        instance.loadChunk(0, -1).join()

        val npc = StaticNpc()
        npc.setInstance(instance, Pos(0.5, 42.0, 0.5)).join()

        return NpcWorld(env, instance, npc)
    }

    private fun List<Entity>.count(entity: Entity) = count { it === entity }

    /** A stand-in for `AbstractNpcEntity`: never moves itself, syncs its controllers on update. */
    private class StaticNpc : Entity(EntityType.ARMOR_STAND) {
        val controllers = CopyOnWriteArrayList<Controller>()

        override fun movementTick() = Unit

        override fun update(time: Long) {
            super.update(time)
            controllers.forEach { it.syncWithNpc(this) }
        }
    }

    private inner class NpcWorld(
        private val env: Env,
        val instance: Instance,
        val npc: StaticNpc,
    ) {
        fun textDisplay() = attach(
            Controller(
                loader,
                "codes.bed.minestom.npc.display.TextDisplayController",
                arrayOf(Component::class.java, Vec::class.java),
                arrayOf(Component.text("hologram"), TEXT_OFFSET),
            )
        )

        fun interaction() = attach(
            Controller(
                loader,
                "codes.bed.minestom.npc.display.InteractionController",
                arrayOf(Vec::class.java),
                arrayOf(INTERACTION_OFFSET),
            )
        )

        fun tick(count: Int) = repeat(count) { env.tick() }

        private fun attach(controller: Controller) = controller.apply {
            attachTo(npc, instance)
            npc.controllers += this
        }
    }

    /**
     * Calls one transformed controller. The class identity differs from the compile-time one
     * because it comes from [MixinTransformingClassLoader], so the calls go through reflection;
     * production code never does.
     */
    private class Controller(
        loader: ClassLoader,
        binaryName: String,
        constructorTypes: Array<Class<*>>,
        constructorArguments: Array<Any?>,
    ) {
        private val type: Class<*> = loader.loadClass(binaryName)
        private val target: Any = type
            .getConstructor(*constructorTypes)
            .newInstance(*constructorArguments)

        private val attachTo = method("attachTo", Entity::class.java, Instance::class.java)
        private val syncWithNpc = method("syncWithNpc", Entity::class.java)
        private val detach = method("detach")
        private val getEntity = method("getEntity")

        fun attachTo(npc: Entity, instance: Instance) {
            attachTo.invoke(target, npc, instance)
        }

        fun syncWithNpc(npc: Entity) {
            syncWithNpc.invoke(target, npc)
        }

        fun detach() {
            detach.invoke(target)
        }

        fun entity() = getEntity.invoke(target) as Entity?

        fun updateOffset(offset: Vec) {
            method("updateOffset", Vec::class.java).invoke(target, offset)
        }

        fun offset() = method("getOffset").invoke(target) as Vec

        private fun method(name: String, vararg types: Class<*>): Method =
            type.getMethod(name, *types)
    }

    private companion object {
        val TEXT_OFFSET = Vec(0.0, 2.15, 0.0)
        val INTERACTION_OFFSET = Vec(0.0, 0.9, 0.0)
    }
}

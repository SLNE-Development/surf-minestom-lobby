package dev.slne.minestom.lobby.server.world.entity

import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.minestom.lobby.api.extension.addListener
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntitySpawnEvent

@Singleton
class NoGravityListener : EventRegistrar {

    override fun register(node: EventNode<Event>) {
        node.addListener<EntitySpawnEvent> { event ->
            if (event.entity !is Player) {
                event.entity.setNoGravity(true)
            }
        }
    }
}
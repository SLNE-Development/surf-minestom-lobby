package dev.slne.minestom.lobby.server.world.handler.impl

import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.extension.addListener
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntitySpawnEvent

@Singleton
class NoGravityHandler {

    fun initialize(node: EventNode<Event>) {
        node.addListener<EntitySpawnEvent> { event ->
            if (event.entity !is Player) {
                event.entity.setNoGravity(true)
            }
        }
    }
}
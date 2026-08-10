package dev.slne.minestom.lobby.server.world.handler

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.world.handler.impl.NoGravityHandler
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode

@Singleton
class EntityHandler @Inject constructor(
    private val noGravityHandler: NoGravityHandler,
) {

    fun initialize(node: EventNode<Event>) {
        noGravityHandler.initialize(node)
    }
}
package dev.slne.minestom.lobby.server.integration.npc

import codes.bed.minestom.npc.StomNPCs
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import net.minestom.server.event.Event
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode

@Singleton
class NpcService : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        val stomNode = EventNode.type("stom-npcs", EventFilter.INSTANCE)
        node.addChild(stomNode)

        StomNPCs.initialize(stomNode)
    }
}
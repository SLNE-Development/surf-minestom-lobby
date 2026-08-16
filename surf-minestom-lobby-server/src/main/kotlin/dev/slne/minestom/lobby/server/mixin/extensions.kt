@file:Suppress("UnstableApiUsage")

package dev.slne.minestom.lobby.server.mixin

import dev.slne.minestom.lobby.server.mixin.extension.MutablePlayerPacketOutEvent
import net.minestom.server.event.player.PlayerPacketOutEvent
import net.minestom.server.network.packet.server.ServerPacket

fun PlayerPacketOutEvent.setPacket(packet: ServerPacket) {
    (this as MutablePlayerPacketOutEvent).`surf$setPacket`(packet)
}
package dev.slne.minestom.lobby.server.packet

import net.minestom.server.network.NetworkBuffer
import net.minestom.server.network.packet.server.ServerPacket

interface OverridingPacket : ServerPacket.Play {

    val overrides: Class<out ServerPacket.Play>
    fun write(buffer: NetworkBuffer)
}

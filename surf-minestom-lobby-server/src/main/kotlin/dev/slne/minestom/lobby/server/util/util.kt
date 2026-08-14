package dev.slne.minestom.lobby.server.util

import net.minestom.server.listener.manager.PacketListenerManager
import net.minestom.server.listener.manager.PacketPlayListenerConsumer
import net.minestom.server.network.packet.client.ClientPacket

inline fun <reified T : ClientPacket> PacketListenerManager.setPlayListener(consumer: PacketPlayListenerConsumer<T>) {
    setPlayListener(T::class.java, consumer)
}
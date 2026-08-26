package dev.slne.minestom.lobby.server.world

import net.minestom.server.instance.Chunk
import net.minestom.server.network.ConnectionState
import net.minestom.server.network.packet.server.CachedPacket

/**
 * Builds and frames the chunk packet a joining player receives, so the first send only copies bytes.
 *
 * @return whether a framed packet is now cached.
 */
@Suppress("UnstableApiUsage")
fun Chunk.warmFullDataPacket(): Boolean {
    val packet = fullDataPacket
    return packet is CachedPacket && packet.body(ConnectionState.PLAY) != null
}

package dev.slne.minestom.lobby.server.bootstrap

import net.minestom.server.MinecraftServer

private const val COMPRESSION_THRESHOLD = 0

fun applyBackendCompressionThreshold() {
    MinecraftServer.setCompressionThreshold(COMPRESSION_THRESHOLD)
    bootstrapLogger.info("Packet compression disabled on the backend connection.")
}

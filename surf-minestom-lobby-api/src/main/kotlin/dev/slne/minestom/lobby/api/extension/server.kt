package dev.slne.minestom.lobby.api.extension

import net.minestom.server.MinecraftServer

val server get() = MinecraftServer.getServer() ?: error("Server is not initialized yet. Please make sure to call this after the server has started.")


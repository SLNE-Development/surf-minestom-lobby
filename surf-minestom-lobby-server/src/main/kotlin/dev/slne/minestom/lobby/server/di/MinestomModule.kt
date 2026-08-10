package dev.slne.minestom.lobby.server.di

import com.google.inject.AbstractModule
import net.minestom.server.MinecraftServer
import net.minestom.server.event.GlobalEventHandler
import net.minestom.server.instance.InstanceManager

class MinestomModule(private val minecraftServer: MinecraftServer) : AbstractModule() {

    override fun configure() {
        bind(MinecraftServer::class.java).toInstance(minecraftServer)
        bind(GlobalEventHandler::class.java).toInstance(MinecraftServer.getGlobalEventHandler())
        bind(InstanceManager::class.java).toInstance(MinecraftServer.getInstanceManager())
    }
}

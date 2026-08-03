package dev.slne.minestom.lobby.server.plugin

import dev.slne.minestom.lobby.api.plugin.MinestomPlugin
import java.util.*

object MinestomPluginLoader {

    fun discover(classLoader: ClassLoader = Thread.currentThread().contextClassLoader): List<MinestomPlugin> {
        return ServiceLoader
            .load(MinestomPlugin::class.java, classLoader)
            .toList()
    }
}
package dev.slne.minestom.lobby.api.plugin

import com.google.inject.AbstractModule
import dev.slne.minestom.lobby.api.plugin.annotation.MinestomPluginMeta

abstract class MinestomPlugin(
    val entrypoint: Class<out MinestomPluginEntrypoint>
) : AbstractModule() {
    val meta = MinestomPluginMeta.get(javaClass)

    init {
        MinestomPluginMeta.validate(meta)
    }

    final override fun configure() {
        configurePlugin()
    }

    /**
     * Register the plugin's Guice bindings here.
     *
     * This method must not start services, open connections or register
     * listeners. Side effects belong in the entrypoint's start method.
     */
    protected open fun configurePlugin() = Unit
}
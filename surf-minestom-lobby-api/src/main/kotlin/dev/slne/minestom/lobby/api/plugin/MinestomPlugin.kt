package dev.slne.minestom.lobby.api.plugin

import com.google.inject.AbstractModule
import dev.slne.minestom.lobby.api.plugin.annotation.MinestomPluginMeta

/**
 * A plugin as the server sees it: a Guice module plus the entrypoint that starts it.
 *
 * Plugins are discovered through `ServiceLoader`, so a subclass needs a
 * `META-INF/services/dev.slne.minestom.lobby.api.plugin.MinestomPlugin` entry, a
 * [MinestomPluginMeta] annotation for its id and dependencies, and a no-argument constructor.
 *
 * Their modules are installed into the server's own injector, which means a plugin can inject
 * anything the server binds - and contribute to the server's own extension points.
 */
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
     * ```
     * override fun configurePlugin() {
     *     bind(MyService::class.java).asEagerSingleton()
     *     binder().bindEventRegistrar<MyListener>()
     *     binder().bindCommandRegistrar<MyCommands>()
     * }
     * ```
     *
     * This method must not start services, open connections or register
     * listeners. Side effects belong in the entrypoint's start method.
     *
     * @see dev.slne.minestom.lobby.api.di.bindEventRegistrar
     * @see dev.slne.minestom.lobby.api.di.bindCommandRegistrar
     */
    protected open fun configurePlugin() = Unit
}
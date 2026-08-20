package dev.slne.minestom.lobby.server.plugin

import com.google.inject.Inject
import com.google.inject.Injector
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.plugin.MinestomPlugin
import dev.slne.minestom.lobby.api.plugin.MinestomPluginEntrypoint

@Singleton
class MinestomPluginManager @Inject constructor(
    private val injector: Injector,
    private val catalog: PluginCatalog,
) {
    private data class StartedPlugin(
        val plugin: MinestomPlugin,
        val entrypoint: MinestomPluginEntrypoint,
    )

    private val startedPlugins = ArrayDeque<StartedPlugin>()

    private var started = false

    suspend fun startAll() {
        check(!started) { "Minestom plugins have already been started" }

        try {
            for (plugin in catalog.plugins) {
                val entrypoint = injector.getInstance(plugin.entrypoint)

                entrypoint.start()

                startedPlugins.addLast(
                    StartedPlugin(
                        plugin = plugin,
                        entrypoint = entrypoint,
                    )
                )
            }

            for ((_, entrypoint) in startedPlugins) {
                entrypoint.afterStart()
            }

            started = true
        } catch (startupFailure: Throwable) {
            rollback(startupFailure)
            throw startupFailure
        }
    }

    suspend fun stopAll() {
        var failure: Throwable? = null

        while (startedPlugins.isNotEmpty()) {
            val startedPlugin = startedPlugins.removeLast()

            try {
                startedPlugin.entrypoint.stop()
            } catch (currentFailure: Throwable) {
                if (failure == null) {
                    failure = currentFailure
                } else {
                    failure.addSuppressed(currentFailure)
                }
            }
        }

        started = false

        failure?.let { throw it }
    }

    private suspend fun rollback(startupFailure: Throwable) {
        while (startedPlugins.isNotEmpty()) {
            val startedPlugin = startedPlugins.removeLast()

            try {
                startedPlugin.entrypoint.stop()
            } catch (rollbackFailure: Throwable) {
                startupFailure.addSuppressed(rollbackFailure)
            }
        }
    }
}
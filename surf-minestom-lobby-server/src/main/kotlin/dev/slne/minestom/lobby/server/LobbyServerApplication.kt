package dev.slne.minestom.lobby.server

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.core.CoreServerInitializer
import dev.slne.minestom.lobby.server.plugin.MinestomPluginManager
import kotlinx.coroutines.runBlocking
import net.minestom.server.MinecraftServer
import java.util.concurrent.atomic.AtomicBoolean

@Singleton
class LobbyServerApplication @Inject constructor(
    private val minecraftServer: MinecraftServer,
    private val config: ServerConfig,
    private val coreServerInitializer: CoreServerInitializer,
    private val pluginManager: MinestomPluginManager,
) {
    private val started = AtomicBoolean()
    private val stopped = AtomicBoolean()


    suspend fun start() {
        check(started.compareAndSet(false, true)) {
            "Lobby server has already been started"
        }

        try {
            coreServerInitializer.initialize()
            pluginManager.startAll()

            installShutdownHook()

            minecraftServer.start(config.address.host, config.address.port)
        } catch (startupFailure: Throwable) {
            runCatching {
                pluginManager.stopAll()
            }.onFailure(startupFailure::addSuppressed)

            runCatching {
                coreServerInitializer.shutdown()
            }.onFailure(startupFailure::addSuppressed)

            throw startupFailure
        }
    }

    suspend fun stop() {
        if (!stopped.compareAndSet(false, true)) {
            return
        }

        var failure: Throwable? = null

        if (MinecraftServer.isStarted() && !MinecraftServer.isStopping()) {
            try {
                MinecraftServer.stopCleanly()
            } catch (currentFailure: Throwable) {
                failure = currentFailure
            }
        }

        try {
            pluginManager.stopAll()
        } catch (currentFailure: Throwable) {
            if (failure == null) {
                failure = currentFailure
            } else {
                failure.addSuppressed(currentFailure)
            }
        }

        try {
            coreServerInitializer.shutdown()
        } catch (currentFailure: Throwable) {
            if (failure == null) {
                failure = currentFailure
            } else {
                failure.addSuppressed(currentFailure)
            }
        }

        failure?.let { throw it }
    }

    private fun installShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
            Thread(
                {
                    runBlocking {
                        runCatching {
                            stop()
                        }.onFailure(Throwable::printStackTrace)
                    }
                },
                "surf-minestom-lobby-shutdown",
            )
        )
    }
}
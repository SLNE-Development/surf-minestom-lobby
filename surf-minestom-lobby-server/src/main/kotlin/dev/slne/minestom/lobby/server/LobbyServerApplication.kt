package dev.slne.minestom.lobby.server

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.console.LobbyTerminalConsole
import dev.slne.minestom.lobby.server.core.CoreServerInitializer
import dev.slne.minestom.lobby.server.plugin.MinestomPluginManager
import kotlinx.coroutines.runBlocking
import net.minestom.server.MinecraftServer
import net.minestom.server.MinecraftServer.LOGGER
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

    private var consoleThread: Thread? = null

    suspend fun start() {
        check(started.compareAndSet(false, true)) {
            "Lobby server has already been started"
        }

        val startupStartedAt = System.nanoTime()

        try {
            LOGGER.info("Initializing core server components.")
            coreServerInitializer.initialize()
            LOGGER.info("Core server components initialized.")

            LOGGER.info("Starting server plugins.")
            pluginManager.startAll()
            LOGGER.info("Server plugins started.")

            installShutdownHook()

            LOGGER.info(
                "Binding server to {}:{}.",
                config.address.host,
                config.address.port,
            )
            minecraftServer.start(config.address.host, config.address.port)

            startConsole()

            val startupDurationMs = (System.nanoTime() - startupStartedAt) / 1_000_000

            LOGGER.info(
                "Surf Minestom Lobby is ready in {} ms.",
                startupDurationMs,
            )
        } catch (startupFailure: Throwable) {
            LOGGER.error(
                "Failed to start Surf Minestom Lobby.",
                startupFailure,
            )

            runCatching {
                LOGGER.info("Stopping plugins after failed startup.")
                pluginManager.stopAll()
            }.onFailure {
                startupFailure.addSuppressed(it)
                LOGGER.error(
                    "Failed to stop plugins after startup failure.",
                    it,
                )
            }

            runCatching {
                LOGGER.info("Shutting down core components after failed startup.")
                coreServerInitializer.shutdown()
            }.onFailure {
                startupFailure.addSuppressed(it)
                LOGGER.error(
                    "Failed to shut down core components after startup failure.",
                    it,
                )
            }

            throw startupFailure
        }
    }

    private fun startConsole() {
        val console = LobbyTerminalConsole {
            runBlocking {
                runCatching {
                    stop()
                }.onFailure {
                    LOGGER.error(
                        "Failed to stop Surf Minestom Lobby from console.",
                        it,
                    )
                }
            }
        }

        consoleThread = Thread(
            console::start,
            "surf-minestom-lobby-console",
        ).apply {
            isDaemon = true
            start()
        }
    }

    suspend fun stop() {
        if (!stopped.compareAndSet(false, true)) {
            return
        }

        LOGGER.info("Stopping Surf Minestom Lobby.")

        var failure: Throwable? = null

        if (MinecraftServer.isStarted() && !MinecraftServer.isStopping()) {
            try {
                MinecraftServer.stopCleanly()
            } catch (currentFailure: Throwable) {
                LOGGER.error(
                    "Failed to stop the Minestom server cleanly.",
                    currentFailure,
                )
                failure = currentFailure
            }
        }

        try {
            LOGGER.info("Stopping server plugins.")
            pluginManager.stopAll()
            LOGGER.info("Server plugins stopped.")
        } catch (currentFailure: Throwable) {
            LOGGER.error("Failed to stop server plugins.", currentFailure,)

            if (failure == null) {
                failure = currentFailure
            } else {
                failure.addSuppressed(currentFailure)
            }
        }

        try {
            LOGGER.info("Shutting down core server components.")
            coreServerInitializer.shutdown()
            LOGGER.info("Core server components shut down.")
        } catch (currentFailure: Throwable) {
            LOGGER.error("Failed to shut down core server components.", currentFailure,)

            if (failure == null) {
                failure = currentFailure
            } else {
                failure.addSuppressed(currentFailure)
            }
        }

        if (failure == null) {
            LOGGER.info("Surf Minestom Lobby stopped successfully.")
        } else {
            throw failure
        }
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
package dev.slne.minestom.lobby.server.lifecycle

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.console.LobbyTerminalConsole
import dev.slne.minestom.lobby.server.plugin.MinestomPluginManager
import dev.slne.minestom.lobby.server.version.LobbyVersionService
import dev.slne.minestom.lobby.server.version.logVersionBanner
import kotlinx.coroutines.runBlocking
import net.minestom.server.MinecraftServer
import net.minestom.server.MinecraftServer.LOGGER
import org.jetbrains.annotations.Blocking
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

@Singleton
class LobbyServerApplication @Inject constructor(
    private val minecraftServer: MinecraftServer,
    private val config: ServerConfig,
    private val serverLifecycle: ServerLifecycle,
    private val pluginManager: MinestomPluginManager,
    private val versionService: LobbyVersionService,
) {
    private val started = AtomicBoolean()
    private val stopped = AtomicBoolean()

    private var consoleThread: Thread? = null

    suspend fun start(startupStartedAt: Long) {
        check(started.compareAndSet(false, true)) {
            "Lobby server has already been started"
        }

        try {
            LOGGER.info("Initializing core server components.")
            serverLifecycle.start()
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

            val startupDuration =
                (System.nanoTime() - startupStartedAt).nanoseconds.inWholeMilliseconds.milliseconds

            LOGGER.info(
                "Surf Minestom Lobby is ready in {}.",
                startupDuration,
            )

            logVersionBanner(versionService, LOGGER)
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
                serverLifecycle.stop()
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
            shutdownAndExit("console")
        }

        consoleThread = Thread(
            console::start,
            "surf-minestom-lobby-console",
        ).apply {
            isDaemon = true
            start()
        }
    }

    fun beginShutdown() {
        if (stopped.get()) return
        thread(isDaemon = false, name = "shutdown-thread") {
            shutdownAndExit("command")
        }
    }

    @Blocking
    private fun shutdownAndExit(source: String) {
        runBlocking {
            runCatching {
                stop()
            }.onFailure {
                LOGGER.error(
                    "Failed to stop Surf Minestom Lobby from {}.",
                    source,
                    it,
                )
            }
        }

        exitProcess(0)
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
            LOGGER.error("Failed to stop server plugins.", currentFailure)

            if (failure == null) {
                failure = currentFailure
            } else {
                failure.addSuppressed(currentFailure)
            }
        }

        try {
            LOGGER.info("Shutting down core server components.")
            serverLifecycle.stop()
            LOGGER.info("Core server components shut down.")
        } catch (currentFailure: Throwable) {
            LOGGER.error("Failed to shut down core server components.", currentFailure)

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
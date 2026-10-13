package dev.slne.minestom.lobby.server.lifecycle

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.codeofconduct.CodeOfConductService
import dev.slne.minestom.lobby.server.command.DefaultCommandRegistrar
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.version.LobbyVersionService
import dev.slne.minestom.lobby.server.version.logVersionBanner
import dev.slne.minestom.lobby.server.world.LobbyWorldService
import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.api.minestom.server.SurfMinestomServer
import dev.slne.surf.api.minestom.server.chat.withSignedChat
import dev.slne.surf.api.minestom.server.console.withConsole
import dev.slne.surf.api.minestom.server.luckperms.withLuckPerms
import dev.slne.surf.api.minestom.server.npc.withNpcLib
import dev.slne.surf.api.minestom.server.plugins.withPlugins
import dev.slne.surf.api.minestom.server.spark.withSpark
import dev.slne.surf.api.minestom.server.surfMinestomServer
import dev.slne.surf.api.minestom.server.visibility.withPlayerVisibility
import kotlinx.coroutines.runBlocking
import net.minestom.server.MinecraftServer
import net.minestom.server.MinecraftServer.LOGGER
import org.jetbrains.annotations.Blocking
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import dev.slne.minestom.lobby.server.bootstrap.exitFlushingLogs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

@Singleton
class LobbyServerApplication @Inject constructor(
    private val minecraftServer: MinecraftServer,
    private val config: ServerConfig,
    private val serverLifecycle: ServerLifecycle,
    private val registrars: Set<@JvmSuppressWildcards EventRegistrar>,
    private val commands: DefaultCommandRegistrar,
    private val codeOfConduct: CodeOfConductService,
    private val versionService: LobbyVersionService,
    private val worldService: LobbyWorldService,
) {
    private val started = AtomicBoolean()
    private val stopped = AtomicBoolean()

    suspend fun start(startupStartedAt: Long) {
        check(started.compareAndSet(false, true)) {
            "Lobby server has already been started"
        }

        try {
            LOGGER.info("Starting the surf api and the lobby services.")
            startSurfApi()
            LOGGER.info("Surf api and lobby services started.")

            commands.register()

            LOGGER.info("Warming chunk packets.")
            worldService.warmChunkPackets()
            LOGGER.info("Chunk packets warmed.")

            installShutdownHook()

            LOGGER.info(
                "Binding server to {}:{}.",
                config.address.host,
                config.address.port,
            )
            minecraftServer.start(config.address.host, config.address.port)

            val startupDuration =
                (System.nanoTime() - startupStartedAt).nanoseconds.inWholeMilliseconds.milliseconds

            LOGGER.info(
                "Surf Minestom Lobby is ready in {}.",
                startupDuration,
            )

            logVersionBanner(versionService, LOGGER)
        } catch (startupFailure: Throwable) {
            runCatching { stop() }.onFailure {
                startupFailure.addSuppressed(it)
                LOGGER.error(
                    "Failed to shut down after startup failure.",
                    it,
                )
            }

            throw startupFailure
        }
    }

    private fun startSurfApi() {
        surfMinestomServer {
            maxPlayers = config.maxPlayers

            withConfigurationPhase {
                codeOfConduct { player -> codeOfConduct.textFor(player.locale) }
            }

            withLuckPerms()
            withSignedChat {
                enforceSecureProfile = config.chat.enforceSecureProfile
                chatSpamThresholdSeconds = config.chat.chatSpamThresholdSeconds
                commandSpamThresholdSeconds = config.chat.commandSpamThresholdSeconds
            }
            withSpark {
                profileOnStartup = config.performance.spark.profileOnStartup
            }
            withNpcLib()

            val visibility = config.playerVisibility
            if (visibility.enabled) {
                withPlayerVisibility {
                    maxVisible = visibility.maxVisible
                    activateAt = visibility.activateAt
                    deactivateBelow = visibility.deactivateBelow
                    refreshIntervalTicks = visibility.refreshIntervalTicks
                    retentionDistanceFactor = visibility.retentionDistanceFactor
                }
            }

            withConsole {
                threadName = "surf-minestom-lobby-console"
                onShutdown = { shutdownAndExit("console") }
            }

            install(LobbyFeature(serverLifecycle))
            withPlugins()

            listeners(*registrars.toTypedArray())
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

        exitFlushingLogs(0)
    }

    /**
     * Stops the server, which disables the surf api together with the plugins and the lobby
     * services.
     */
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
                failure = failure.alsoSuppress(currentFailure)
            }
        }

        // Runs on its own when the server stops, but not when it never started
        try {
            runCatching { SurfMinestomServer.instance }.getOrNull()?.shutdown()
        } catch (currentFailure: Throwable) {
            LOGGER.error("Failed to shut down the surf api.", currentFailure)
            failure = failure.alsoSuppress(currentFailure)
        }

        // The surf api already stopped them, unless it failed to load after starting them
        try {
            serverLifecycle.stop()
        } catch (currentFailure: Throwable) {
            LOGGER.error("Failed to shut down the lobby services.", currentFailure)
            failure = failure.alsoSuppress(currentFailure)
        }

        if (failure == null) {
            LOGGER.info("Surf Minestom Lobby stopped successfully.")
        } else {
            throw failure
        }
    }

    private fun Throwable?.alsoSuppress(next: Throwable): Throwable {
        if (this == null) return next
        addSuppressed(next)
        return this
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

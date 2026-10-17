package dev.slne.minestom.lobby.server.lifecycle

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.codeofconduct.CodeOfConductService
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
import net.minestom.server.entity.EntityType
import org.jetbrains.annotations.Blocking
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

/**
 * Starts and stops the lobby: its own services, the surf api with its features and plugins, and
 * the Minestom server.
 *
 * Startup order: the core services, including the lobby world, then the surf api, which loads and
 * enables the plugins, then the integration services, then the server binds its socket. A plugin
 * that fails to load or enable fails the startup.
 *
 * Once the surf api runs, [SurfMinestomServer.stop] stops the server: Minestom, the surf api and
 * the plugins, then the lobby services as a shutdown action. The console, `/stop` and the JVM
 * shutdown hook of the surf api all lead to it.
 */
@Singleton
class LobbyServerApplication @Inject constructor(
    private val minecraftServer: MinecraftServer,
    private val config: ServerConfig,
    private val serverLifecycle: ServerLifecycle,
    private val versionService: LobbyVersionService,
    private val worldService: LobbyWorldService,
    private val codeOfConduct: CodeOfConductService,
    private val registrars: Set<@JvmSuppressWildcards EventRegistrar>,
) {
    private val started = AtomicBoolean()

    @Volatile
    private var api: SurfMinestomServer? = null

    suspend fun start(startupStartedAt: Long) {
        check(started.compareAndSet(false, true)) {
            "Lobby server has already been started"
        }

        val surfApi = try {
            LOGGER.info("Initializing core server components.")
            serverLifecycle.startCore()
            LOGGER.info("Core server components initialized.")

            LOGGER.info("Starting the surf api and the plugins.")
            startSurfApi()
        } catch (startupFailure: Throwable) {
            // Without a running surf api, no shutdown action stops the services
            failStartup(startupFailure) { serverLifecycle.stop() }
        }
        api = surfApi

        try {
            LOGGER.info("Initializing server integrations.")
            serverLifecycle.startIntegrations()

            LOGGER.info("Warming chunk packets.")
            worldService.warmChunkPackets()
            LOGGER.info("Chunk packets warmed.")

            LOGGER.info(
                "Binding server to {}:{}.",
                config.address.host,
                config.address.port,
            )
            minecraftServer.start(config.address.host, config.address.port)
        } catch (startupFailure: Throwable) {
            failStartup(startupFailure) { surfApi.stop() }
        }

        val startupDuration =
            (System.nanoTime() - startupStartedAt).nanoseconds.inWholeMilliseconds.milliseconds

        LOGGER.info(
            "Surf Minestom Lobby is ready in {}.",
            startupDuration,
        )

        logVersionBanner(versionService, LOGGER)
    }

    private suspend fun failStartup(startupFailure: Throwable, stop: suspend () -> Unit): Nothing {
        LOGGER.error(
            "Failed to start Surf Minestom Lobby.",
            startupFailure,
        )

        try {
            stop()
        } catch (stopFailure: Throwable) {
            startupFailure.addSuppressed(stopFailure)
        }

        throw startupFailure
    }

    private fun startSurfApi(): SurfMinestomServer = surfMinestomServer {
        maxPlayers = config.maxPlayers
        disableEntityTicking(
            config.performance.nonTickingEntityTypes.map { key ->
                EntityType.fromKey(key) ?: error("Unknown entity type: $key")
            }
        )

        withConfigurationPhase {
            codeOfConduct { player -> codeOfConduct.textFor(player.locale) }
        }

        withSignedChat {
            enforceSecureProfile = config.chat.enforceSecureProfile
            chatSpamThresholdSeconds = config.chat.chatSpamThresholdSeconds
            commandSpamThresholdSeconds = config.chat.commandSpamThresholdSeconds
        }

        withLuckPerms()

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
        }

        withPlugins {
            requireAll = true
        }

        listeners(*registrars.toTypedArray())

        onShutdown(::stopServices)
    }

    @Blocking
    private fun stopServices() {
        LOGGER.info("Shutting down core server components.")
        runBlocking { serverLifecycle.stop() }
        LOGGER.info("Core server components shut down.")
    }

    /**
     * Stops the server on a thread of its own and exits the process.
     *
     * [SurfMinestomServer.stop] waits until the plugins are disabled, which the tick thread running
     * a player's command must not wait for.
     */
    fun beginShutdown() {
        val surfApi = api ?: return

        // Not a daemon, even when started from the console thread, which is one
        thread(isDaemon = false, name = "lobby-shutdown", start = true) {
            surfApi.stop()
            exitProcess(0)
        }
    }
}

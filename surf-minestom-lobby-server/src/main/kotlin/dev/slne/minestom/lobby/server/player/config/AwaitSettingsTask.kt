package dev.slne.minestom.lobby.server.player.config

import com.google.inject.Singleton
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerSettingsChangeEvent
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Waits until the client's settings have arrived.
 */
@Singleton
class AwaitSettingsTask : ConfigurationTask {

    private companion object {
        val LOGGER = ComponentLogger.logger()

        /** How long the client gets to send its settings before the defaults are used. */
        const val SETTINGS_TIMEOUT_SECONDS = 2L
    }

    private val received = ConcurrentHashMap<UUID, CompletableFuture<Unit>>()

    override fun run(context: ConfigurationContext) {
        try {
            settingsFuture(context.player.uuid)
                .get(SETTINGS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (_: TimeoutException) {
            LOGGER.warn(
                "Player {} did not send their settings, continuing with the defaults",
                context.player.username
            )
        }
    }

    private fun settingsFuture(uuid: UUID): CompletableFuture<Unit> =
        received.computeIfAbsent(uuid) { CompletableFuture() }

    fun handleSettingsChange(event: PlayerSettingsChangeEvent) {
        settingsFuture(event.player.uuid).complete(Unit)
    }

    fun handleDisconnect(event: PlayerDisconnectEvent) {
        received.remove(event.player.uuid)?.complete(Unit)
    }
}

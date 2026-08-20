package dev.slne.minestom.lobby.server.player.config

import net.minestom.server.entity.Player
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.instance.Instance
import org.jetbrains.annotations.Blocking

/**
 * One step of the configuration phase, run in order by [LobbyConfiguration].
 */
interface ConfigurationTask {

    val taskName: String get() = javaClass.simpleName

    /** Runs the step and blocks until the client has answered it. */
    @Blocking
    fun run(context: ConfigurationContext)
}

class ConfigurationContext(
    val player: Player,
    val isFirstConfig: Boolean,
    val event: AsyncPlayerConfigurationEvent,
    val spawningInstance: Instance,
)

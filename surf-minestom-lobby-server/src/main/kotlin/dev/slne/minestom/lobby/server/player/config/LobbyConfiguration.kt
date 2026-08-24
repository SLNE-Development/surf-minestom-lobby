package dev.slne.minestom.lobby.server.player.config

import dev.slne.minestom.lobby.api.extension.ConnectionManager
import dev.slne.minestom.lobby.api.player.requireLobbyPlayer
import dev.slne.minestom.lobby.server.duck.ConnectionManagerDuck
import dev.slne.minestom.lobby.server.player.PlayerLoginGate
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import net.minestom.server.MinecraftServer
import net.minestom.server.ServerFlag
import net.minestom.server.entity.Player
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.network.ConnectionManager
import net.minestom.server.network.packet.server.common.PluginMessagePacket
import org.jetbrains.annotations.Blocking

object LobbyConfiguration {
    @Volatile
    private var tasks: List<ConfigurationTask> = emptyList()

    @Volatile
    private lateinit var loginGate: PlayerLoginGate

    fun install(tasks: List<ConfigurationTask>, loginGate: PlayerLoginGate) {
        this.tasks = ObjectImmutableList(tasks)
        this.loginGate = loginGate
    }

    @Blocking
    @JvmStatic
    fun doConfiguration(player: Player, isFirstConfig: Boolean) {
        check(ServerFlag.INSIDE_TEST || Thread.currentThread().isVirtual) { "doConfiguration must run on a virtual thread" }

        if (isFirstConfig) {
            ConnectionManager.duck().`surf$configurationPlayers`().add(player)
            ConnectionManager.duck().`surf$keepAlivePlayers`().add(player)

            player.refreshKeepAlive(System.nanoTime())
            player.refreshAnswerKeepAlive(true)

            if (!loginGate.admit(player.requireLobbyPlayer())) return
        }

        player.sendPacket(PluginMessagePacket.brandPacket(MinecraftServer.getBrandName()))

        val event = AsyncPlayerConfigurationEvent(player, isFirstConfig)
        EventDispatcher.call(event)

        if (!player.isOnline) return

        val spawningInstance = requireNotNull(event.spawningInstance) {
            "You need to specify a spawning instance in the AsyncPlayerConfigurationEvent"
        }

        val context = ConfigurationContext(player, isFirstConfig, event, spawningInstance)

        for (task in tasks) {
            if (!player.isOnline) return
            task.run(context)
        }
    }

    internal fun stopKeepAlive(player: Player) {
        ConnectionManager.duck().`surf$keepAlivePlayers`().remove(player)
    }

    @Suppress("CAST_NEVER_SUCCEEDS", "NOTHING_TO_INLINE")
    private inline fun ConnectionManager.duck() = this as ConnectionManagerDuck
}

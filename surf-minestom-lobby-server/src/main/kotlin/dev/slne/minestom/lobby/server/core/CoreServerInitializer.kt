package dev.slne.minestom.lobby.server.core

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.command.CommandRegistrar
import dev.slne.minestom.lobby.api.extension.addListener
import dev.slne.minestom.lobby.api.instance.LobbyInstance
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.console.LobbyTerminalConsole
import dev.slne.minestom.lobby.server.luckperms.LuckPermsService
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.GlobalEventHandler
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.instance.InstanceContainer
import revxrsal.commands.minestom.MinestomLamp
import java.util.concurrent.atomic.AtomicBoolean

@Singleton
class CoreServerInitializer @Inject constructor(
    private val config: ServerConfig,
    private val globalEventHandler: GlobalEventHandler,

    @LobbyInstance
    private val lobbyInstance: InstanceContainer,
    private val commands: Set<@JvmSuppressWildcards CommandRegistrar>,

    private val luckperms: LuckPermsService
) {
    private val initialized = AtomicBoolean()

    private val eventNode: EventNode<Event> = EventNode.all("surf-minestom-lobby:core")

    fun initialize() {
        check(initialized.compareAndSet(false, true)) {
            "Core server has already been initialized"
        }

        registerCommands()
        registerEvents()
    }

    fun shutdown() {
        if (!initialized.compareAndSet(true, false)) {
            return
        }

        globalEventHandler.removeChild(eventNode)
        luckperms.close()
    }

    private fun registerEvents() {

        eventNode.addListener<AsyncPlayerConfigurationEvent> { event ->
            event.spawningInstance = lobbyInstance
            event.player.respawnPoint = config.spawn.toPos()
        }

        globalEventHandler.addChild(eventNode)
    }

    private fun registerCommands() {
        val lamp = MinestomLamp.builder().build()

        lamp.register()

        for (registrar in commands) {
            registrar.register(lamp)
        }
    }
}
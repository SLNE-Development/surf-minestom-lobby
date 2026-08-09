package dev.slne.minestom.lobby.server.core

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.command.CommandService
import dev.slne.minestom.lobby.server.luckperms.LuckPermsService
import dev.slne.minestom.lobby.server.permission.PermissionLevelService
import dev.slne.minestom.lobby.server.player.LobbyPlayerService
import dev.slne.minestom.lobby.server.player.chat.ChatService
import dev.slne.minestom.lobby.server.player.handler.LobbyPlayerHandler
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.GlobalEventHandler
import java.util.concurrent.atomic.AtomicBoolean

@Singleton
class CoreServerInitializer @Inject constructor(
    private val globalEventHandler: GlobalEventHandler,

    private val luckperms: LuckPermsService,
    private val lobbyPlayerService: LobbyPlayerService,
    private val commandService: CommandService,
    private val permissionLevelService: PermissionLevelService,
    private val lobbyPlayerHandler: LobbyPlayerHandler,
    private val chatService: ChatService,
) {
    private val initialized = AtomicBoolean()

    private val eventNode: EventNode<Event> = EventNode.all("surf-minestom-lobby:core")

    fun initialize() {
        check(initialized.compareAndSet(false, true)) {
            "Core server has already been initialized"
        }

        lobbyPlayerService.registerPlayerProvider()
        commandService.register()
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
        permissionLevelService.initialize(eventNode)
        lobbyPlayerHandler.initialize(eventNode)
        chatService.initialize(eventNode)

        globalEventHandler.addChild(eventNode)
    }
}
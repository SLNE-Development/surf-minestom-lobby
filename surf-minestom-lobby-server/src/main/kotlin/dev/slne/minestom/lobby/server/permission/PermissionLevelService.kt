package dev.slne.minestom.lobby.server.permission

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.minestom.lobby.api.extension.ConnectionManager
import dev.slne.minestom.lobby.api.extension.addListener
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import dev.slne.minestom.lobby.api.player.getOnlineLobbyPlayerByUuid
import dev.slne.minestom.lobby.api.player.lobbyPlayer
import dev.slne.minestom.lobby.server.integration.luckperms.LuckPermsService
import dev.slne.minestom.lobby.server.lifecycle.LobbyService
import net.luckperms.api.event.EventSubscription
import net.luckperms.api.event.user.UserDataRecalculateEvent
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerSpawnEvent

@Singleton
class PermissionLevelService @Inject constructor(
    private val luckPerms: LuckPermsService,
) : LobbyService, EventRegistrar {

    private var subscription: EventSubscription<UserDataRecalculateEvent>? = null

    override suspend fun start() {
        subscription = luckPerms.luckPerms.eventBus.subscribe(
            UserDataRecalculateEvent::class.java
        ) { event ->
            val player = ConnectionManager.getOnlineLobbyPlayerByUuid(event.user.uniqueId)
                ?: return@subscribe

            player.scheduleNextTick { apply(player) }
        }
    }

    override suspend fun stop() {
        subscription?.close()
        subscription = null
    }

    override fun register(node: EventNode<Event>) {
        node.addListener<PlayerSpawnEvent> { event -> apply(event.lobbyPlayer) }
    }

    private fun apply(player: LobbyPlayer) {
        player.permissionLevel = resolveLevel(player)
    }

    private fun resolveLevel(player: LobbyPlayer): Int {
        for (level in LobbyPermissions.MAX_OP_LEVEL downTo 1) {
            if (player.hasPermission(LobbyPermissions.opLevel(level))) return level
        }

        return 0
    }
}

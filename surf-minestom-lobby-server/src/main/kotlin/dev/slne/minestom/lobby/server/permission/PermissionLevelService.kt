package dev.slne.minestom.lobby.server.permission

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.coroutine.launch
import dev.slne.minestom.lobby.api.coroutine.minestomAsyncScope
import dev.slne.minestom.lobby.api.extension.ConnectionManager
import dev.slne.minestom.lobby.api.extension.addListener
import dev.slne.minestom.lobby.api.extension.server
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import dev.slne.minestom.lobby.api.player.getOnlineLobbyPlayerByUuid
import dev.slne.minestom.lobby.server.luckperms.LuckPermsService
import kotlinx.coroutines.launch
import net.luckperms.api.event.user.UserDataRecalculateEvent
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerSpawnEvent

@Singleton
class PermissionLevelService @Inject constructor(
    private val luckPerms: LuckPermsService,
) {

    fun initialize(eventNode: EventNode<Event>) {
        eventNode.addListener<PlayerSpawnEvent> { event ->
            val player = event.player
            require(player is LobbyPlayer)
            apply(player)
        }

        luckPerms.luckPerms.eventBus.subscribe(UserDataRecalculateEvent::class.java) { event ->
            val player = ConnectionManager.getOnlineLobbyPlayerByUuid(event.user.uniqueId)
                ?: return@subscribe

            player.scheduleNextTick {
                apply(player)
            }
        }
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

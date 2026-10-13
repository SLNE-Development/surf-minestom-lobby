package dev.slne.minestom.lobby.server.permission

import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.lifecycle.LobbyService
import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.api.minestom.extension.ConnectionManager
import dev.slne.surf.api.minestom.extension.addListener
import dev.slne.surf.api.minestom.permission.hasPermission
import dev.slne.surf.api.minestom.server.luckperms.LuckPermsFeature
import net.luckperms.api.event.EventSubscription
import net.luckperms.api.event.user.UserDataRecalculateEvent
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerSpawnEvent

/**
 * Derives each player's vanilla permission level, which the client uses to show operator features,
 * from the `minestom.lobby.op.<level>` permissions.
 */
@Singleton
class PermissionLevelService : LobbyService, EventRegistrar {

    private var subscription: EventSubscription<UserDataRecalculateEvent>? = null

    override suspend fun start() {
        subscription = LuckPermsFeature.instance.luckPerms.eventBus.subscribe(
            UserDataRecalculateEvent::class.java
        ) { event ->
            val player = ConnectionManager.getOnlinePlayerByUuid(event.user.uniqueId)
                ?: return@subscribe

            player.scheduleNextTick { apply(player) }
        }
    }

    override suspend fun stop() {
        subscription?.close()
        subscription = null
    }

    override fun register(node: EventNode<Event>) {
        node.addListener<PlayerSpawnEvent> { event -> apply(event.player) }
    }

    private fun apply(player: Player) {
        player.permissionLevel = resolveLevel(player)
    }

    private fun resolveLevel(player: Player): Int {
        for (level in LobbyPermissions.MAX_OP_LEVEL downTo 1) {
            if (player.hasPermission(LobbyPermissions.opLevel(level))) return level
        }

        return 0
    }
}

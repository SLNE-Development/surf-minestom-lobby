package dev.slne.minestom.lobby.server.integration.miniplaceholders

import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.lifecycle.LobbyService
import dev.slne.surf.api.minestom.server.luckperms.LuckPermsFeature
import io.github.miniplaceholders.minestom.MinestomMiniPlaceholders
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.util.TriState
import net.minestom.server.entity.Player
import kotlin.io.path.Path

/**
 * Runs MiniPlaceholders, resolving its permission checks with LuckPerms.
 *
 * Starts once the surf api runs, because LuckPerms is loaded by it.
 */
@Singleton
class MiniPlaceholdersService : LobbyService {

    override suspend fun start() {
        val luckPerms = LuckPermsFeature.instance

        MinestomMiniPlaceholders.initialize(DATA_DIRECTORY) { sender, permission ->
            permissionValue(luckPerms, sender, permission)
        }
    }

    /**
     * MiniPlaceholders grants everything but [TriState.FALSE], so a player LuckPerms has not
     * loaded holds nothing instead of [TriState.NOT_SET].
     */
    private fun permissionValue(luckPerms: LuckPermsFeature, sender: Audience, permission: String): TriState {
        if (sender !is Player) return TriState.TRUE
        if (luckPerms.loadedUser(sender.uuid) == null) return TriState.FALSE

        return luckPerms.permissionValue(sender.uuid, permission)
    }

    private companion object {
        val DATA_DIRECTORY = Path("plugins/miniplaceholders")
    }
}

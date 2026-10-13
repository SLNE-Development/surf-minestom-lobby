package dev.slne.minestom.lobby.server.integration.miniplaceholders

import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.lifecycle.LobbyService
import dev.slne.surf.api.minestom.permission.MinestomPermissions
import io.github.miniplaceholders.minestom.MinestomMiniPlaceholders
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.util.TriState
import net.minestom.server.entity.Player
import kotlin.io.path.Path

@Singleton
class MiniPlaceholdersService : LobbyService {

    override suspend fun start() {
        MinestomMiniPlaceholders.initialize(DATA_DIRECTORY, ::permissionValue)
    }

    private fun permissionValue(sender: Audience, permission: String): TriState {
        if (sender !is Player) return TriState.TRUE

        return MinestomPermissions.permissionValue(sender, permission)
    }

    private companion object {
        val DATA_DIRECTORY = Path("plugins/miniplaceholders")
    }
}

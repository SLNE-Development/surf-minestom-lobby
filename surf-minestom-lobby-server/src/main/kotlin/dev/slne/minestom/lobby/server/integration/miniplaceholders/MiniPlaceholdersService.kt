package dev.slne.minestom.lobby.server.integration.miniplaceholders

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.integration.luckperms.LuckPermsService
import dev.slne.minestom.lobby.server.lifecycle.LobbyService
import io.github.miniplaceholders.minestom.MinestomMiniPlaceholders
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.util.TriState
import net.luckperms.api.util.Tristate
import net.minestom.server.entity.Player
import kotlin.io.path.Path

@Singleton
class MiniPlaceholdersService @Inject constructor(
    private val luckPerms: LuckPermsService,
) : LobbyService {

    override suspend fun start() {
        MinestomMiniPlaceholders.initialize(DATA_DIRECTORY, ::permissionValue)
    }

    private fun permissionValue(sender: Audience, permission: String): TriState {
        if (sender !is Player) return TriState.TRUE

        return luckPerms.hasPermission(sender.uuid, permission).toTriState()
    }

    private fun Tristate.toTriState() = when (this) {
        Tristate.TRUE -> TriState.TRUE
        Tristate.FALSE -> TriState.FALSE
        Tristate.UNDEFINED -> TriState.NOT_SET
    }

    private companion object {
        val DATA_DIRECTORY = Path("plugins/miniplaceholders")
    }
}

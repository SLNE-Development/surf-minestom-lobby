package dev.slne.minestom.lobby.server.integration.luckperms

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.integration.luckperms.config.LuckpermsConfigAdapter
import dev.slne.minestom.lobby.server.lifecycle.LobbyService
import me.lucko.luckperms.minestom.CommandRegistry
import me.lucko.luckperms.minestom.LuckPermsMinestom
import net.kyori.adventure.util.TriState
import net.luckperms.api.LuckPerms
import net.luckperms.api.model.user.User
import net.luckperms.api.util.Tristate
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.div

@Singleton
class LuckPermsService @Inject constructor() : LobbyService {

    private var instance: LuckPerms? = null

    val luckPerms: LuckPerms
        get() = checkNotNull(instance) {
            "LuckPermsService has not been started yet - it has to come first in ServerLifecycle"
        }

    override suspend fun start() {
        instance = LuckPermsMinestom.builder(DATA_DIRECTORY)
            .commandRegistry(CommandRegistry.minestom())
            .configurationAdapter { plugin ->
                LuckpermsConfigAdapter(plugin, DATA_DIRECTORY / "config.yml")
            }
            .enable()
    }

    override suspend fun stop() {
        if (instance == null) return

        instance = null
        LuckPermsMinestom.disable()
    }

    fun hasPermission(uuid: UUID, permission: String): Tristate {
        val user = getLoadedUser(uuid) ?: return Tristate.FALSE
        return user.cachedData.permissionData.checkPermission(permission)
    }

    fun getLoadedUser(uuid: UUID): User? = luckPerms.userManager.getUser(uuid)

    private companion object {
        val DATA_DIRECTORY = Path("plugins/luckperms")
    }
}

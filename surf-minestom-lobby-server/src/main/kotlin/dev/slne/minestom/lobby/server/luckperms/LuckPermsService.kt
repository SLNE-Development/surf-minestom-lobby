package dev.slne.minestom.lobby.server.luckperms

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.luckperms.config.LuckpermsConfigAdapter
import me.lucko.luckperms.minestom.CommandRegistry
import me.lucko.luckperms.minestom.LuckPermsMinestom
import net.luckperms.api.model.user.User
import java.lang.AutoCloseable
import java.util.UUID
import kotlin.io.path.Path
import kotlin.io.path.div

@Singleton
class LuckPermsService @Inject constructor() : AutoCloseable {

    val luckpermsDir = Path("plugins/luckperms")
    val luckPerms = LuckPermsMinestom.builder(luckpermsDir)
        .commandRegistry(CommandRegistry.minestom())
        .configurationAdapter { plugin ->
            LuckpermsConfigAdapter(plugin, luckpermsDir / "config.yml")
        }
        .enable()

    override fun close() {
    }

    fun hasPermission(uuid: UUID, permission: String): Boolean {
        val user = getLoadedUser(uuid) ?: return false
        return user.cachedData.permissionData.checkPermission(permission).asBoolean()
    }

    fun getLoadedUser(uuid: UUID): User? {
        return luckPerms.userManager.getUser(uuid)
    }
}
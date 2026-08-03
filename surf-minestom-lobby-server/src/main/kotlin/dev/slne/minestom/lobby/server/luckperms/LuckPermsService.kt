package dev.slne.minestom.lobby.server.luckperms

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.luckperms.config.LuckpermsConfigAdapter
import me.lucko.luckperms.minestom.CommandRegistry
import me.lucko.luckperms.minestom.LuckPermsMinestom
import java.lang.AutoCloseable
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
}
package dev.slne.minestom.lobby.server.integration.spark

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.extension.CommandManager
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.integration.luckperms.LuckPermsService
import dev.slne.minestom.lobby.server.lifecycle.LobbyService
import me.lucko.spark.minestom.MinestomSparkPlugin
import net.minestom.server.entity.Player
import kotlin.io.path.Path

@Singleton
class SparkService @Inject constructor(
    private val luckPerms: LuckPermsService,
    private val config: ServerConfig.SparkConfig,
) : LobbyService {

    private var plugin: MinestomSparkPlugin? = null

    override suspend fun start() {
        val spark = MinestomSparkPlugin(DATA_DIRECTORY) { sender, permission ->
            sender !is Player || luckPerms.hasPermission(sender.uuid, permission)
        }

        spark.enable()
        plugin = spark

        if (config.profileOnStartup) {
            CommandManager.execute(CommandManager.consoleSender, PROFILER_START_COMMAND)
        }
    }

    override suspend fun stop() {
        plugin?.disable()
        plugin = null
    }

    private companion object {
        val DATA_DIRECTORY = Path("plugins/spark")
        const val PROFILER_START_COMMAND = "spark profiler start --thread *"
    }
}

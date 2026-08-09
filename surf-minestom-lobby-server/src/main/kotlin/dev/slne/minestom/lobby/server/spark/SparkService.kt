package dev.slne.minestom.lobby.server.spark

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.extension.CommandManager
import dev.slne.minestom.lobby.server.luckperms.LuckPermsService
import me.lucko.spark.minestom.MinestomSparkPlugin
import net.minestom.server.entity.Player
import kotlin.io.path.Path

@Singleton
class SparkService @Inject constructor(
    private val luckperms: LuckPermsService
) : AutoCloseable {

    val sparkDir = Path("plugins/spark")
    val spark = MinestomSparkPlugin(sparkDir) { sender, permission ->
        sender !is Player || luckperms.hasPermission(sender.uuid, permission)
    }

    fun init() {
        spark.enable()
        CommandManager.execute(CommandManager.consoleSender, "spark profiler start --thread *")
    }

    override fun close() {
        spark.disable()
    }
}
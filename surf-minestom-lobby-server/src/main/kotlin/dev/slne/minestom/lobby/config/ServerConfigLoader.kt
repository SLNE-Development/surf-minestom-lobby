package dev.slne.minestom.lobby.config

import org.spongepowered.configurate.kotlin.dataClassFieldDiscoverer
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolute

class ServerConfigLoader(private val path: Path) {

    @Volatile
    var config: ServerConfig? = null
        private set

    private val loader = YamlConfigurationLoader.builder()
        .path(path)
        .indent(2)
        .defaultOptions { options ->
            options.serializers { builder ->
                builder.registerAnnotatedObjects(
                    ObjectMapper.factoryBuilder()
                        .addDiscoverer(dataClassFieldDiscoverer())
                        .build()
                )
            }
        }
        .build()

    fun load(): ServerConfig {
        path.parent?.let(Files::createDirectories)

        val fileExisted = Files.exists(path)
        val root = loader.load()


        val migrated = ConfigMigrations.migrate(root)

        val config = if (fileExisted) {
            root.get<ServerConfig>()
                ?: error("Could not deserialize configuration at ${path.absolute()}")
        } else {
            ServerConfig()
        }

        validate(config)

        if (!fileExisted || migrated) {
            root.set(config)
            loader.save(root)
        }

        this.config = config

        return config
    }

    private fun validate(config: ServerConfig) {

    }

    fun getConfig(): ServerConfig {
        return config ?: error("Configuration not loaded. Call load() first.")
    }

    companion object {
        private lateinit var manager: ServerConfigLoader

        fun load(path: Path) {
            manager = ServerConfigLoader(path)
            manager.load()
        }

        fun get(): ServerConfig {
            return manager.getConfig()
        }
    }
}

val serverConfig get() = ServerConfigLoader.get()
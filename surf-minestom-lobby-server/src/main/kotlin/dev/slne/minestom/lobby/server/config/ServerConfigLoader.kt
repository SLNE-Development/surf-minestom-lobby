package dev.slne.minestom.lobby.server.config

import dev.slne.minestom.lobby.server.config.constraints.NonBlank
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.dataClassFieldDiscoverer
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolute

class ServerConfigLoader(private val path: Path) {
    private val loader = YamlConfigurationLoader.builder()
        .path(path)
        .indent(2)
        .nodeStyle(NodeStyle.BLOCK)
        .defaultOptions { options ->
            options.serializers { serializers ->
                serializers.registerAnnotatedObjects(
                    ObjectMapper.factoryBuilder()
                        .addDiscoverer(dataClassFieldDiscoverer())
                        .addConstraint(NonBlank::class.java, String::class.java, NonBlank.Factory)
                        .build()
                )
            }
        }
        .build()

    fun load(beforeDeserialize: (ConfigurationNode) -> Unit = {}): ServerConfig {
        path.parent?.let(Files::createDirectories)

        val fileExisted = Files.exists(path)
        val root = loader.load()
        ConfigMigrations.migrate(root)
        beforeDeserialize(root)

        val config = if (fileExisted) {
            root.get<ServerConfig>()
                ?: error("Could not deserialize configuration at ${path.absolute()}")
        } else {
            ServerConfig()
        }

        root.set(config)
        loader.save(root)

        return config
    }
}
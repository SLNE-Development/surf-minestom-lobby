package dev.slne.minestom.lobby.server.config

import org.spongepowered.configurate.CommentedConfigurationNode

object ConfigMigrations {

    fun migrate(root: CommentedConfigurationNode): Boolean {
        var version = root.node("_config-version").getInt(0)
        var changed = false

        while (version < ServerConfig.CURRENT_VERSION) {
            when (version) {
                1 -> Unit
                else -> error("Unsupported configuration version $version")
            }

            version++
            root.node("_config-version").set(version)
            changed = true
        }

        require(version <= ServerConfig.CURRENT_VERSION) {
            "The configuration has version $version, but this server only " +
                    "supports up to version ${ServerConfig.CURRENT_VERSION}."
        }

        return changed
    }

    private fun move(
        root: CommentedConfigurationNode,
        source: Array<String>,
        target: Array<String>,
    ) {
        val sourceNode = root.node(*source)

        if (sourceNode.virtual()) {
            return
        }

        val targetNode = root.node(*target)

        if (targetNode.virtual()) {
            targetNode.from(sourceNode)
        }

        sourceNode.raw(null)
    }
}
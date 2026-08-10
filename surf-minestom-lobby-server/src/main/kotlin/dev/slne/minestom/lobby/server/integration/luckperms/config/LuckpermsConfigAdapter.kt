package dev.slne.minestom.lobby.server.integration.luckperms.config

import io.leangen.geantyref.TypeToken
import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter
import me.lucko.luckperms.common.plugin.LuckPermsPlugin
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.getList
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.FileOutputStream
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists

class LuckpermsConfigAdapter(
    private val plugin: LuckPermsPlugin,
    private val path: Path
) : ConfigurationAdapter {

    private val loader = YamlConfigurationLoader.builder()
        .path(path)
        .nodeStyle(NodeStyle.BLOCK)
        .build()

    private var root: ConfigurationNode? = null

    init {
        if (!path.exists()) {
            val default = javaClass.getResourceAsStream("/luckperms.yml")
                ?: error("Default luckperms.yml not found in resources")
            path.createParentDirectories()
            FileOutputStream(path.toFile()).use { outputStream ->
                default.transferTo(outputStream)
            }
        }

        reload()
    }

    override fun reload() {
        root = loader.load()
    }

    override fun getString(path: String, def: String?): String? {
        val node = resolvePath(path)
        return if (def != null) node.getString(def) else node.string
    }

    override fun getInteger(path: String, def: Int): Int {
        return resolvePath(path).getInt(def)
    }

    override fun getBoolean(path: String, def: Boolean): Boolean {
        return resolvePath(path).getBoolean(def)
    }

    override fun getStringList(
        path: String,
        def: List<String>?
    ): List<String>? {
        val node = resolvePath(path)
        if (node.virtual() || !node.isList) {
            return def
        }

        return if (def == null) node.getList(String::class) else node.getList(String::class, def)
    }

    override fun getStringMap(
        path: String,
        def: Map<String, String>?
    ): Map<String, String>? {
        val node = resolvePath(path)
        if (node.virtual()) {
            return def
        }
        val type = object : TypeToken<Map<String, @JvmSuppressWildcards String>>() {}
        return if (def == null) node.get(type) else node.get(type, def)
    }

    private fun resolvePath(path: String): ConfigurationNode {
        val root = root
        requireNotNull(root) { "Config is not loaded." }
        return root.node(*path.split(".").toTypedArray())
    }

    override fun getPlugin(): LuckPermsPlugin {
        return plugin
    }
}
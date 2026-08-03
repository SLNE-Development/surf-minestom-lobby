package dev.slne.minestom.lobby.server.plugin

import dev.slne.minestom.lobby.api.plugin.MinestomPlugin

class PluginCatalog(discoveredPlugins: Collection<MinestomPlugin>) {
    val plugins = PluginDependencyResolver.resolve(discoveredPlugins)
}
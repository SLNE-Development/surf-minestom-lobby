package dev.slne.minestom.lobby.server.plugin

import dev.slne.minestom.lobby.api.plugin.MinestomPlugin
import java.util.*

object PluginDependencyResolver {

    fun resolve(plugins: Collection<MinestomPlugin>): List<MinestomPlugin> {
        val pluginsById = plugins.groupBy { it.meta.id }

        val duplicateIds = pluginsById
            .filterValues { pluginsForId -> pluginsForId.size > 1 }
            .keys

        require(duplicateIds.isEmpty()) {
            "Duplicate Minestom plugin ids: ${duplicateIds.sorted().joinToString()}"
        }

        val byId = pluginsById.mapValues { (_, pluginsForId) ->
            pluginsForId.single()
        }

        val missingDependencies = buildList {
            for (plugin in plugins) {
                for (dependency in plugin.meta.dependsOn) {
                    if (dependency !in byId) {
                        add("${plugin.meta.id} -> $dependency")
                    }
                }
            }
        }

        require(missingDependencies.isEmpty()) {
            "Missing Minestom plugin dependencies: ${missingDependencies.sorted().joinToString()}"
        }

        val remainingDependencies = byId.mapValuesTo(HashMap()) { (_, plugin) ->
            plugin.meta.dependsOn.toMutableSet()
        }

        val dependents = HashMap<String, MutableSet<String>>()


        for (plugin in plugins) {
            for (dependency in plugin.meta.dependsOn) {
                dependents
                    .getOrPut(dependency, ::linkedSetOf)
                    .add(plugin.meta.id)
            }
        }

        val ready = PriorityQueue<String>()

        for ((id, dependencies) in remainingDependencies) {
            if (dependencies.isEmpty()) {
                ready.add(id)
            }
        }

        val result = ArrayList<MinestomPlugin>(plugins.size)

        while (ready.isNotEmpty()) {
            val id = ready.remove()
            result += byId.getValue(id)

            for (dependentId in dependents[id].orEmpty()) {
                val dependencies = remainingDependencies.getValue(dependentId)

                dependencies.remove(id)

                if (dependencies.isEmpty()) {
                    ready.add(dependentId)
                }
            }
        }

        if (result.size != plugins.size) {
            val unresolved = remainingDependencies
                .filterValues { it.isNotEmpty() }
                .entries
                .sortedBy { it.key }
                .joinToString { (id, dependencies) ->
                    "$id -> ${dependencies.sorted()}"
                }

            error("Cyclic Minestom plugin dependencies detected: $unresolved")
        }

        return result
    }
}
package dev.slne.minestom.lobby.server.command.commandapi.brigadier

import dev.slne.minestom.lobby.api.command.commandapi.CommandDefinition
import dev.slne.minestom.lobby.api.command.commandapi.CommandPath
import dev.slne.minestom.lobby.api.command.commandapi.argument.ArgumentDefinition
import dev.slne.minestom.lobby.api.command.commandapi.argument.ArgumentKind
import dev.slne.minestom.lobby.server.command.commandapi.MinestomConditions
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minestom.server.command.CommandSender
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket

private const val NODE_ROOT = 0
private const val NODE_LITERAL = 1
private const val NODE_ARGUMENT = 2
private const val NODE_EXECUTABLE = 0x04

/**
 * Builds the command tree a client is shown, merging the CommandAPI's commands into the packet
 * Minestom produced for its own.
 *
 * Nodes a sender may not use are left out entirely, so a command the sender cannot run does not
 * appear in its completions.
 */
internal class DeclareCommandsMerger(
    private val tree: BrigadierCommandTree,
    private val conditions: MinestomConditions = MinestomConditions(),
    private val declarations: NodeDeclarations = NodeDeclarations(),
) {
    fun merge(original: DeclareCommandsPacket, sender: CommandSender): DeclareCommandsPacket {
        val roots = tree.registered().mapNotNull { (labels, definition) ->
            treeFor(definition, sender)?.let { branches -> labels to branches }
        }
        if (roots.isEmpty()) return original

        val nodes = original.nodes().mapTo(ObjectArrayList(), ::copyOf)
        val extraRootChildren = IntArrayList()

        roots.forEach { (labels, branches) ->
            labels.forEach { label ->
                val node = DeclareCommandsPacket.Node()
                node.flags = flagsOf(NODE_LITERAL, branches.executable)
                node.name = label
                nodes += node

                val index = nodes.size - 1
                node.children = branches.children.values.map { child -> emit(child, nodes) }.toIntArray()
                extraRootChildren.add(index)
            }
        }

        val root = nodes[original.rootIndex()]
        root.children += extraRootChildren.toIntArray()

        return DeclareCommandsPacket(nodes, original.rootIndex())
    }

    private fun emit(source: PendingNode, nodes: MutableList<DeclareCommandsPacket.Node>): Int {
        val node = DeclareCommandsPacket.Node()
        val definition = source.definition

        if (definition == null) {
            node.flags = flagsOf(NODE_LITERAL, source.executable)
            node.name = checkNotNull(source.literal)
        } else {
            val declaration = declarations.of(definition)
            node.flags = flagsOf(NODE_ARGUMENT, source.executable)
            node.name = definition.nodeName
            node.parser = declaration.parser
            node.properties = declaration.properties
            node.suggestionsType = declaration.suggestionsType
        }

        nodes += node
        val index = nodes.size - 1
        node.children = source.children.values.map { child -> emit(child, nodes) }.toIntArray()
        return index
    }

    private fun flagsOf(type: Int, executable: Boolean): Byte {
        val flags = if (executable) type or NODE_EXECUTABLE else type
        return flags.toByte()
    }

    private fun copyOf(source: DeclareCommandsPacket.Node): DeclareCommandsPacket.Node {
        val node = DeclareCommandsPacket.Node()
        node.flags = source.flags
        node.children = source.children.copyOf()
        node.redirectedNode = source.redirectedNode
        node.name = source.name
        node.parser = source.parser
        node.properties = source.properties
        node.suggestionsType = source.suggestionsType
        return node
    }

    /**
     * Folds every path of [definition] into one tree, dropping whatever [sender] may not use.
     *
     * Paths that share a prefix collapse onto the same nodes, the way they do in the dispatcher, so
     * the client sees one branch rather than one per declared path.
     */
    private fun treeFor(definition: CommandDefinition, sender: CommandSender): PendingNode? {
        val root = PendingNode(literal = null, definition = null)
        var any = false

        definition.paths.forEach { path ->
            if (!conditions.canUse(sender, path.permissions, path.requirements)) return@forEach

            val depths = executableDepths(path)
            var current = root
            if (0 in depths) {
                root.executable = true
                any = true
            }

            for ((index, argument) in path.arguments.withIndex()) {
                if (!conditions.canUse(sender, argument.permissions, argument.requirements)) return@forEach

                current = current.child(argument)
                if (index + 1 in depths) {
                    current.executable = true
                    any = true
                }
            }
        }

        return root.takeIf { any }
    }

    private fun executableDepths(path: CommandPath): IntRange {
        val firstOptional = path.arguments.indexOfFirst(ArgumentDefinition<*>::optional)
        return if (firstOptional == -1) {
            path.arguments.size..path.arguments.size
        } else {
            firstOptional..path.arguments.size
        }
    }

    private class PendingNode(val literal: String?, val definition: ArgumentDefinition<*>?) {
        var executable = false
        val children = LinkedHashMap<String, PendingNode>()

        fun child(argument: ArgumentDefinition<*>): PendingNode {
            val kind = argument.kind
            val literal = (kind as? ArgumentKind.Literal)?.literal
            val key = if (literal != null) "literal:$literal" else "argument:${argument.nodeName}"

            return children.getOrPut(key) {
                PendingNode(
                    literal = literal,
                    definition = if (literal == null) argument else null,
                )
            }
        }
    }
}

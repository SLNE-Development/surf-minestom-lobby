package dev.slne.minestom.lobby.server.command.commandapi

import com.mojang.brigadier.ParseResults
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContextBuilder
import com.mojang.brigadier.tree.ArgumentCommandNode
import dev.slne.minestom.lobby.server.chat.signature.PlayerChatMessage
import dev.slne.minestom.lobby.server.command.commandapi.brigadier.SignedMessageArgumentType
import dev.slne.minestom.lobby.server.command.commandapi.brigadier.SuggestingArgumentType
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import net.minestom.server.command.CommandSender
import net.minestom.server.crypto.ArgumentSignatures

/**
 * The messages the sender of the command being dispatched signed, keyed by argument node name.
 */
internal object SignedCommandArguments {
    private val current = ThreadLocal<Map<String, PlayerChatMessage>>()

    /** Runs [block] with [messages] visible to every signed message argument parsed within it. */
    fun <T> withMessages(messages: Map<String, PlayerChatMessage>, block: () -> T): T {
        val previous = current.get()
        current.set(messages)

        try {
            return block()
        } finally {
            if (previous == null) current.remove() else current.set(previous)
        }
    }

    /** The message signed for the argument named [nodeName], or `null` when none was. */
    fun find(nodeName: String): PlayerChatMessage? = current.get()?.get(nodeName)
}

/**
 * The text a client signs for [input], keyed by the node name of the argument it is read from.
 */
internal fun signableCommandArguments(
    sender: CommandSender,
    input: String,
): Map<String, String> {
    val ownership = MinestomCommandAPIPlatform.activeOwnership() ?: return emptyMap()
    val dispatcher = MinestomCommandAPIPlatform.activeDispatcher() ?: return emptyMap()
    if (ownership.findInput(input) == null) return emptyMap()

    return signableArgumentsOf(dispatcher.parse(input.removePrefix("/"), sender))
}

/**
 * The signable arguments of [parse], keyed by node name, in the order they were read.
 */
internal fun signableArgumentsOf(
    parse: ParseResults<CommandSender>,
): Map<String, String> {
    val input = parse.reader.string
    val values = Object2ObjectLinkedOpenHashMap<String, String>()

    var context: CommandContextBuilder<CommandSender>? = parse.context
    while (context != null) {
        context.nodes.forEach { parsed ->
            val node = parsed.node
            if (node is ArgumentCommandNode<*, *> && node.type.isSignable()) {
                values[node.name] = input.substring(parsed.range.start, parsed.range.end)
            }
        }

        context = context.child
    }

    return values
}

/**
 * Whether [signatures] covers exactly the signable arguments in [values].
 */
internal fun signaturesCoverArguments(
    signatures: ArgumentSignatures,
    values: Map<String, String>,
): Boolean {
    val entries = signatures.entries()
    return entries.size == values.size && entries.all { entry -> entry.name() in values }
}

/** Whether a node of this type is one the client signs. */
private fun ArgumentType<*>.isSignable(): Boolean = when (this) {
    is SuggestingArgumentType -> delegate is SignedMessageArgumentType
    else -> this is SignedMessageArgumentType
}

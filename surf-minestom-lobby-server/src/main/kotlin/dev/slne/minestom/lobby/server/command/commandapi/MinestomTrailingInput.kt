package dev.slne.minestom.lobby.server.command.commandapi

import dev.slne.minestom.lobby.api.command.commandapi.CommandPath
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.adventure.MinestomAdventure
import net.minestom.server.command.builder.CommandContext

/**
 * Detects command input that the matched syntax left unconsumed and renders the vanilla syntax
 * error for it.
 *
 * Minestom's parser selects the deepest syntax it can satisfy and ignores any remaining input, so a
 * syntax may execute while part of the command line was never read.
 */
internal object MinestomTrailingInput {
    private const val CONTEXT_LENGTH = 10
    private const val HERE_KEY = "command.context.here"
    private const val UNKNOWN_COMMAND_KEY = "command.unknown.command"
    private const val UNKNOWN_ARGUMENT_KEY = "command.unknown.argument"

    /**
     * Returns the index of the first character that [context] did not consume for [path], or `null`
     * when the input was fully consumed.
     *
     * Also returns `null` when the consumed extent cannot be reconstructed, because a raw argument
     * value no longer matches the input it came from. Callers then execute as before rather than
     * reject valid input.
     */
    fun unconsumedFrom(
        context: CommandContext,
        path: CommandPath,
        fixedByName: Map<String, String>,
    ): Int? {
        val input = context.input
        var cursor = endOfToken(input, 0)

        for (definition in path.arguments) {
            val name = definition.nodeName
            val raw = when {
                fixedByName.containsKey(name) -> fixedByName.getValue(name)
                context.has(name) -> context.getRaw(name)
                else -> null
            } ?: continue

            val start = skipSpaces(input, cursor)
            if (!input.startsWith(raw, start)) return null
            cursor = start + raw.length
        }

        val remainder = skipSpaces(input, cursor)
        return remainder.takeIf { it < input.length }
    }

    /**
     * Builds the vanilla syntax failure for [input] with the error positioned at [cursor].
     *
     * [consumedArgument] selects between vanilla's two messages: an input whose command label was
     * followed by nothing readable is an unknown command, while trailing data after at least one
     * accepted argument is an incorrect argument.
     */
    fun syntaxError(input: String, cursor: Int, consumedArgument: Boolean): Component {
        val clamped = cursor.coerceIn(0, input.length)
        val context = Component.text()
            .color(NamedTextColor.GRAY)
            .clickEvent(ClickEvent.suggestCommand("/$input"))

        if (clamped > CONTEXT_LENGTH) {
            context.append(Component.text("..."))
        }
        context.append(Component.text(input.substring((clamped - CONTEXT_LENGTH).coerceAtLeast(0), clamped)))
        if (clamped < input.length) {
            context.append(
                Component.text(input.substring(clamped))
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.UNDERLINED),
            )
        }
        context.append(
            Component.translatable(HERE_KEY)
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.ITALIC),
        )

        val messageKey = if (consumedArgument) UNKNOWN_ARGUMENT_KEY else UNKNOWN_COMMAND_KEY

        return Component.text()
            .append(Component.translatable(messageKey, NamedTextColor.RED))
            .append(Component.newline())
            .append(context)
            .build()
    }

    /**
     * Whether [path] accepted at least one argument or literal from [context].
     */
    fun consumedArgument(
        context: CommandContext,
        path: CommandPath,
        fixedByName: Map<String, String>,
    ): Boolean = path.arguments.any { definition ->
        fixedByName.containsKey(definition.nodeName) || context.has(definition.nodeName)
    }

    private fun endOfToken(input: String, from: Int): Int {
        var index = from
        while (index < input.length && !input[index].isWhitespace()) index++
        return index
    }

    private fun skipSpaces(input: String, from: Int): Int {
        var index = from
        while (index < input.length && input[index].isWhitespace()) index++
        return index
    }
}

package dev.slne.minestom.lobby.server.command.commandapi

import dev.slne.minestom.lobby.api.command.commandapi.argument.InputShape

internal data class SuggestionRange(
    val start: Int,
    val length: Int,
    val current: String,
)

internal object SuggestionCursor {
    fun scan(
        input: String,
        shape: InputShape,
        delimiter: Char? = null,
        argumentStart: Int? = null,
    ): SuggestionRange {
        val logical = input.removeSuffix("\u0000")
        val tokenStart = when (shape) {
            InputShape.GREEDY -> requireNotNull(argumentStart) {
                "Greedy suggestions require argumentStart"
            }

            InputShape.QUOTED -> quotedTokenStart(logical)
            InputShape.WORD -> logical.lastIndexOf(' ').plus(1)
        }

        var elementStart = delimiter?.let { logical.lastIndexOf(it) + 1 }
            ?.coerceAtLeast(tokenStart)
            ?: tokenStart

        while (elementStart < logical.length && logical[elementStart].isWhitespace()) {
            elementStart++
        }

        return SuggestionRange(
            start = elementStart + 1,
            length = logical.length - elementStart,
            current = logical.substring(elementStart),
        )
    }

    private fun quotedTokenStart(input: String): Int {
        var tokenStart = input.lastIndexOf(' ').plus(1)
        var quoted = false
        var escaped = false

        input.forEachIndexed { index, character ->
            when {
                escaped -> escaped = false
                character == '\\' -> escaped = true
                character == '"' -> {
                    quoted = !quoted
                    if (quoted) tokenStart = index + 1
                }

                character.isWhitespace() && !quoted -> tokenStart = index + 1
            }
        }
        return tokenStart
    }
}

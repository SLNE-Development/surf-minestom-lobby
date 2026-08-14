/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.minestom.lobby.api.command.commandapi.argument

import it.unimi.dsi.fastutil.objects.ObjectList

class StringArgument(nodeName: String) : Argument<String>(nodeName) {
    override val kind = ArgumentKind.Word
    override fun stringify(value: String): String = value
}

class TextArgument(nodeName: String) : Argument<String>(nodeName) {
    override val kind = ArgumentKind.Text
    override val inputShape = InputShape.QUOTED
    override fun stringify(value: String): String = value.asQuotablePhrase()
}

class GreedyStringArgument(nodeName: String) : Argument<String>(nodeName) {
    override val kind = ArgumentKind.GreedyString
    override val inputShape = InputShape.GREEDY
    override fun stringify(value: String): String = value
}

class LiteralArgument(
    nodeName: String,
    val literal: String = nodeName,
) : Argument<String>(nodeName) {
    override val kind = ArgumentKind.Literal(literal)

    init {
        require(literal.isNotBlank()) { "Literal value must not be blank" }
        require(literal.none(Char::isWhitespace)) { "Literal value must be a single token" }
    }

    override fun stringify(value: String): String = value
}

class MultiLiteralArgument(nodeName: String, vararg literals: String) : Argument<String>(nodeName) {
    private val literals: List<String> = ObjectList.of(*literals)
    override val kind = ArgumentKind.MultiLiteral(this.literals)

    init {
        require(this.literals.isNotEmpty()) { "Multi-literal argument must contain at least one literal" }
        require(this.literals.all(String::isNotBlank)) { "Literal values must not be blank" }
        require(this.literals.none { literal -> literal.any(Char::isWhitespace) }) {
            "Multi-literal values must be single tokens"
        }
        require(this.literals.distinct().size == this.literals.size) { "Literal values must be unique" }
    }

    override fun stringify(value: String): String = value
}

class CommandArgument(nodeName: String) : Argument<String>(nodeName) {
    override val kind = ArgumentKind.Command
    override val inputShape = InputShape.GREEDY

    override fun stringify(value: String): String = value
}

private fun String.asQuotablePhrase(): String {
    if (none { character ->
            character.isWhitespace() ||
                    Character.isISOControl(character) ||
                    character == '\'' ||
                    character == '"'
        }
    ) {
        return this
    }

    return buildString(length + 2) {
        append('"')
        this@asQuotablePhrase.forEach { character ->
            append(
                when (character) {
                    '\\' -> "\\\\"
                    '"' -> "\\\""
                    '\b' -> "\\b"
                    '\t' -> "\\t"
                    '\n' -> "\\n"
                    '\u000C' -> "\\f"
                    '\r' -> "\\r"
                    else -> if (Character.isISOControl(character)) {
                        "\\u${character.code.toString(16).padStart(4, '0')}"
                    } else {
                        character
                    }
                },
            )
        }
        append('"')
    }
}

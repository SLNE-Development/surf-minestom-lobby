package dev.slne.minestom.lobby.server.command.commandapi

import dev.slne.minestom.lobby.api.command.commandapi.argument.ArgumentDefinition
import dev.slne.minestom.lobby.api.command.commandapi.argument.ArgumentKind
import dev.slne.minestom.lobby.api.command.commandapi.argument.SuggestionMode

internal tailrec fun suggestionOwner(definition: ArgumentDefinition<*>): ArgumentDefinition<*> {
    if (definition.suggestions != SuggestionMode.BuiltIns) return definition
    return when (val kind = definition.kind) {
        is ArgumentKind.Custom<*, *> -> suggestionOwner(kind.base)
        is ArgumentKind.List<*> -> suggestionOwner(kind.element)
        else -> definition
    }
}

internal tailrec fun builtInDefinition(definition: ArgumentDefinition<*>): ArgumentDefinition<*> =
    when (val kind = definition.kind) {
        is ArgumentKind.Custom<*, *> -> builtInDefinition(kind.base)
        is ArgumentKind.List<*> -> builtInDefinition(kind.element)
        else -> definition
    }

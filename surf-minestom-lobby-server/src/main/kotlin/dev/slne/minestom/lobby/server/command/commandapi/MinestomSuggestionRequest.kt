package dev.slne.minestom.lobby.server.command.commandapi

import dev.slne.minestom.lobby.api.command.commandapi.suggestion.StringTooltip

internal data class MinestomSuggestionRequest(
    val commandName: String,
    val argumentName: String,
    val input: String,
    val range: SuggestionRange,
    val providerDescription: String,
    val resolve: suspend () -> List<StringTooltip>,
)

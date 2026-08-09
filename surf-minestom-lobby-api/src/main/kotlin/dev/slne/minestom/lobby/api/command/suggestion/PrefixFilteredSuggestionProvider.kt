package dev.slne.minestom.lobby.api.command.suggestion

import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.minestom.actor.MinestomCommandActor
import revxrsal.commands.node.ExecutionContext

class PrefixFilteredSuggestionProvider(
    private val candidates: (context: ExecutionContext<MinestomCommandActor>) -> Collection<String>,
) : SuggestionProvider<MinestomCommandActor> {

    override fun getSuggestions(
        context: ExecutionContext<MinestomCommandActor>,
    ): Collection<String> = filterByPrefix(candidates(context), context.currentArgument())
}

/**
 * Returns only the [candidates] whose start matches [typed], compared case-insensitively.
 * A blank [typed] returns all candidates unchanged.
 */
fun filterByPrefix(candidates: Collection<String>, typed: String): Collection<String> {
    if (typed.isEmpty()) return candidates
    return candidates.filter { it.startsWith(typed, ignoreCase = true) }
}

/**
 * The portion of the current argument the user has typed so far: the last
 * whitespace-separated token of the raw command input, or an empty string when a
 * separating space was just typed (i.e. a fresh argument is being started).
 */
fun ExecutionContext<MinestomCommandActor>.currentArgument(): String {
    val source = input().source()
    val lastSpace = source.lastIndexOf(' ')
    return if (lastSpace < 0) source else source.substring(lastSpace + 1)
}

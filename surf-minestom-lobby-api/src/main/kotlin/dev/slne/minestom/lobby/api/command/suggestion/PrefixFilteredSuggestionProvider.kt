package dev.slne.minestom.lobby.api.command.suggestion

import net.minestom.server.command.builder.suggestion.Suggestion
import net.minestom.server.command.builder.suggestion.SuggestionEntry
import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.minestom.actor.MinestomCommandActor
import revxrsal.commands.node.ExecutionContext

/**
 * A suggestion provider implementation that supports generating suggestions based on
 * a prefix filter. It compares candidate strings to the currently typed argument
 * in a command execution context, filtering out candidates that do not start
 * with the provided prefix. The comparison is case-insensitive.
 *
 * @property limit The maximum number of suggestions to return. Defaults to [Int.MAX_VALUE],
 * allowing all matching candidates to be returned. Must not be negative.
 * @property candidates A lambda function that retrieves a collection of candidate strings
 * based on the provided command execution context.
 */
class PrefixFilteredSuggestionProvider(
    private val limit: Int = Int.MAX_VALUE,
    private val candidates: (context: ExecutionContext<MinestomCommandActor>) -> Collection<String>,
) : SuggestionProvider<MinestomCommandActor> {

    init {
        require(limit >= 0) { "limit must not be negative" }
    }

    override fun getSuggestions(
        context: ExecutionContext<MinestomCommandActor>,
    ): Collection<String> = matchingSuggestions(
        candidates(context),
        context.currentArgument(),
        limit,
    ).toList()
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
 * Retrieves the current argument being typed by extracting the substring after the last space
 * in the source input of the command execution context.
 *
 * @return A string representing the current argument. If no spaces are present in the source input,
 * the entire input is returned.
 */
fun ExecutionContext<MinestomCommandActor>.currentArgument(): String {
    val source = input().source()
    val lastSpace = source.lastIndexOf(' ')
    return if (lastSpace < 0) source else source.substring(lastSpace + 1)
}

/**
 * Extracts the current argument being typed in the suggestion input by obtaining
 * the substring after the last space character in the input string.
 *
 * @return The current argument as a string. If there are no spaces present in the
 * input, the entire input string is returned.
 */
fun Suggestion.currentArgument(): String = input.substringAfterLast(' ')

/**
 * Adds distinct, case-insensitively prefix-filtered candidates to this suggestion.
 */
fun Suggestion.addPrefixFiltered(
    candidates: Collection<String>,
    limit: Int = Int.MAX_VALUE,
) {
    require(limit >= 0) { "limit must not be negative" }

    matchingSuggestions(candidates, currentArgument(), limit)
        .forEach { addEntry(SuggestionEntry(it)) }
}

private fun matchingSuggestions(
    candidates: Collection<String>,
    prefix: String,
    limit: Int,
): Sequence<String> = candidates.asSequence()
    .distinctBy { it.lowercase() }
    .filter { it.startsWith(prefix, ignoreCase = true) }
    .take(limit)

package dev.slne.minestom.lobby.server.command.commandapi

import net.minestom.server.command.builder.suggestion.Suggestion

internal data class CapturedCompletion(
    val request: MinestomSuggestionRequest?,
    val nativeSuggestion: Suggestion?,
)

internal object CompletionCapture {
    private class Slot(
        val inputOverride: String?,
        var request: MinestomSuggestionRequest? = null,
    )

    private val current = ThreadLocal<Slot?>()

    fun capture(
        inputOverride: String? = null,
        block: () -> Suggestion?,
    ): CapturedCompletion {
        check(current.get() == null) { "Nested suggestion capture is not supported" }
        val slot = Slot(inputOverride)
        current.set(slot)
        return try {
            val nativeSuggestion = block()
            CapturedCompletion(slot.request, nativeSuggestion)
        } finally {
            current.remove()
        }
    }

    fun record(request: MinestomSuggestionRequest): Boolean {
        val slot = current.get() ?: return false
        check(slot.request == null) { "Minestom produced multiple active suggestion callbacks" }
        slot.request = request
        return true
    }

    fun logicalInput(nativeInput: String): String =
        current.get()?.inputOverride ?: nativeInput.removeSuffix("\u0000")
}

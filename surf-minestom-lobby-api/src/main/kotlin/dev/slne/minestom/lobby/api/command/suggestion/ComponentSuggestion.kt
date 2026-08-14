package dev.slne.minestom.lobby.api.command.suggestion

import net.kyori.adventure.text.Component
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.arguments.Argument
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.suggestion.SuggestionEntry
import revxrsal.commands.minestom.actor.MinestomCommandActor
import revxrsal.commands.minestom.argument.ArgumentTypeFactory
data class ComponentSuggestion(
    val value: String,
    val tooltip: Component? = null,
)

fun interface ComponentSuggestionProvider<A : Annotation> {
    fun suggestions(
        annotation: A,
        sender: CommandSender,
        input: String,
    ): Collection<ComponentSuggestion>
}

/** Creates a native Minestom string argument whose entries may have individual Adventure tooltips. */
class ComponentSuggestionArgumentTypeFactory<A : Annotation>(
    private val annotationType: Class<A>,
    private val provider: ComponentSuggestionProvider<A>,
) : ArgumentTypeFactory<MinestomCommandActor> {
    override fun getArgumentType(
        parameter: revxrsal.commands.node.ParameterNode<MinestomCommandActor, *>,
    ): Argument<*>? {
        val annotation = parameter.annotations().get(annotationType) ?: return null
        if (parameter.type() != String::class.java) return null

        return ArgumentType.String(parameter.name()).apply {
            setSuggestionCallback { sender, context, suggestion ->
                val currentInput = context.input.substringAfterLast(' ')
                provider.suggestions(annotation, sender, currentInput)
                    .asSequence()
                    .filter { it.value.startsWith(currentInput, ignoreCase = true) }
                    .distinctBy { it.value.lowercase() }
                    .forEach { entry ->
                        suggestion.addEntry(SuggestionEntry(entry.value, entry.tooltip))
                    }
            }
        }
    }
}

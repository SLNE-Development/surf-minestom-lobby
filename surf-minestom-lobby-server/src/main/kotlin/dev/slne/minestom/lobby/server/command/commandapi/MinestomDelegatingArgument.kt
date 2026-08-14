package dev.slne.minestom.lobby.server.command.commandapi

import dev.slne.minestom.lobby.api.command.commandapi.argument.CustomArgumentInfo
import dev.slne.minestom.lobby.api.command.commandapi.exception.CommandSyntaxException
import dev.slne.minestom.lobby.api.command.commandapi.exception.WrapperCommandSyntaxException
import net.kyori.adventure.text.Component
import net.minestom.server.command.ArgumentParserType
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.arguments.Argument

/**
 * Keeps a native parser's behavior while presenting a different argument type to Minestom's
 * command graph converter: either a non-specialized type carrying the same wire parser (to
 * advertise `minecraft:ask_server` for custom suggestions), or a distinct parser type entirely
 * (to advertise vanilla metadata, such as `rotation` or `angle`, that this instance's own
 * [convert] logic reproduces on top of a differently-shaped native parser).
 *
 * [nodeProperties] mirrors [base]'s live value unless [nodePropertiesOverride] is supplied, in
 * which case that value is used instead.
 */
internal class MinestomDelegatingArgument<T, B>(
    id: String,
    private val base: Argument<B>,
    private val parserOverride: ArgumentParserType = base.parser(),
    private val nodePropertiesOverride: (() -> ByteArray?)? = null,
    private val convert: (CustomArgumentInfo<B>) -> T,
) : Argument<T>(id, base.allowSpace(), base.useRemaining()) {
    init {
        suggestionType = base.suggestionType()
        base.callback?.let(::setCallback)
        base.defaultValue?.let { defaultValue ->
            setDefaultValue { sender ->
                convert(CustomArgumentInfo(sender, "", defaultValue.apply(sender)))
            }
        }
        base.suggestionCallback?.let(::setSuggestionCallback)
    }

    override fun parse(sender: CommandSender, input: String): T = try {
        convert(CustomArgumentInfo(sender, input, base.parse(sender, input)))
    } catch (failure: WrapperCommandSyntaxException) {
        throw componentFailure(failure.exception, input)
    } catch (failure: CommandSyntaxException) {
        throw componentFailure(failure, input)
    }

    override fun parser(): ArgumentParserType = parserOverride

    override fun nodeProperties(): ByteArray? =
        if (nodePropertiesOverride != null) nodePropertiesOverride.invoke() else base.nodeProperties()

    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
    override fun toString(): String = base.toString()

    private fun componentFailure(failure: CommandSyntaxException, input: String) =
        ComponentArgumentSyntaxException(
            component = failure.component ?: Component.text(failure.message ?: "Invalid argument"),
            message = failure.message ?: "Invalid argument",
            input = failure.input ?: input,
            errorCode = CUSTOM_ERROR,
        )

    private companion object {
        const val CUSTOM_ERROR = 1_220
    }
}

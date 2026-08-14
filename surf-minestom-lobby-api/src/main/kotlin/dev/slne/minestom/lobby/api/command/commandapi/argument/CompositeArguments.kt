/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.minestom.lobby.api.command.commandapi.argument

import dev.slne.minestom.lobby.api.command.commandapi.CommandAPI
import net.minestom.server.command.CommandSender

data class CustomArgumentInfo<B>(
    val sender: CommandSender,
    val currentInput: String,
    val baseValue: B,
)

open class CustomArgument<T, B> internal constructor(
    private val base: Argument<B>,
    private val formatter: (T) -> String,
    private val parser: (CustomArgumentInfo<B>) -> T,
) : Argument<T>(base.nodeName) {
    constructor(
        base: Argument<B>,
        parser: (CustomArgumentInfo<B>) -> T,
    ) : this(base, { value -> value.toString() }, parser)

    init {
        val baseDefinition = base.toDefinition()
        val defaultValue = baseDefinition.defaultValue
        when {
            defaultValue != null -> setOptional { sender ->
                parser(CustomArgumentInfo(sender, "", defaultValue(sender)))
            }

            baseDefinition.optional -> setOptional(optional = true)
        }
        baseDefinition.permissions.forEach(::withPermission)
        baseDefinition.requirements.forEach(::withRequirement)
    }

    override val kind: ArgumentKind<T>
        get() = ArgumentKind.Custom(base.toDefinition(), parser)

    override val inputShape: InputShape
        get() = base.toDefinition().inputShape

    override val listDelimiter: Char?
        get() = base.toDefinition().listDelimiter

    override fun stringify(value: T): String = formatter(value)
}

class ListArgument<T>(
    nodeName: String,
    private val element: Argument<T>,
    val delimiter: Char = ',',
    val allowEmpty: Boolean = false,
) : Argument<List<T>>(nodeName) {
    init {
        val elementDefinition = element.toDefinition()
        require(!delimiter.isWhitespace() && delimiter != '\u0000') {
            "List delimiter must be a visible non-whitespace character"
        }
        require(elementDefinition.inputShape != InputShape.GREEDY) {
            "List elements cannot consume remaining input"
        }
        elementDefinition.permissions.forEach(::withPermission)
        elementDefinition.requirements.forEach(::withRequirement)
    }

    override val kind: ArgumentKind<List<T>>
        get() = ArgumentKind.List(element.toDefinition(), delimiter, allowEmpty)

    override val inputShape = InputShape.GREEDY

    override val listDelimiter: Char = delimiter

    override fun stringify(value: List<T>): String {
        val elementDefinition = element.toDefinition()
        return value.joinToString(delimiter.toString(), transform = elementDefinition.stringify)
    }
}

fun <T, R> Argument<T>.map(mapper: (T) -> R): Argument<R> =
    CustomArgument(this, parser = { info -> mapper(info.baseValue) })

fun <T> Argument<T>.filter(predicate: (T) -> Boolean): Argument<T> {
    val definition = toDefinition()
    return CustomArgument(
        base = this,
        parser = { info ->
            if (!predicate(info.baseValue)) {
                CommandAPI.failWithString("Invalid value for $nodeName")
            }
            info.baseValue
        },
        formatter = definition.stringify,
    )
}

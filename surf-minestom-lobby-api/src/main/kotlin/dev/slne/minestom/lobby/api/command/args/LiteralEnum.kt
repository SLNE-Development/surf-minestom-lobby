package dev.slne.minestom.lobby.api.command.args

import net.minestom.server.command.builder.arguments.ArgumentEnum

/**
 * Marks an `enum` command parameter to be rendered as **literal** command nodes (one per
 * enum constant) instead of Lamp's default string argument.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class LiteralEnum(
    val format: ArgumentEnum.Format = ArgumentEnum.Format.LOWER_CASED,
)

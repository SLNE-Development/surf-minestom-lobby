package dev.slne.minestom.lobby.api.command

/**
 * Requires [permission] for the complete Minestom command tree rooted at the
 * annotated command.
 *
 * The annotated command must contain only the root literal. Child commands keep
 * using [CommandPermission] for their own additional requirements.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RootCommandPermission(val permission: String)

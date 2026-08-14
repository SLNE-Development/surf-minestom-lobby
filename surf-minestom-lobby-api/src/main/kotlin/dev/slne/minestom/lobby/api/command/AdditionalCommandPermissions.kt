package dev.slne.minestom.lobby.api.command

import revxrsal.commands.annotation.DistributeOnMethods
import revxrsal.commands.annotation.NotSender

/**
 * Adds requirements to the permission declared with [CommandPermission].
 * Every listed permission must be granted.
 */
@DistributeOnMethods
@NotSender.ImpliesNotSender
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AdditionalCommandPermissions(vararg val permissions: String)

package dev.slne.minestom.lobby.api.command

import revxrsal.commands.annotation.DistributeOnMethods
import revxrsal.commands.annotation.NotSender

@DistributeOnMethods
@NotSender.ImpliesNotSender
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CommandPermission(val permission: String)

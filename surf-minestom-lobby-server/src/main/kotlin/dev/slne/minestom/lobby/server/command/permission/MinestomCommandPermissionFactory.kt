package dev.slne.minestom.lobby.server.command.permission

import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import revxrsal.commands.Lamp
import revxrsal.commands.annotation.list.AnnotationList
import revxrsal.commands.command.CommandPermission
import revxrsal.commands.minestom.actor.MinestomCommandActor
import dev.slne.minestom.lobby.api.command.CommandPermission as CommandPermissionAnnotation

@Singleton
class MinestomCommandPermissionFactory : CommandPermission.Factory<MinestomCommandActor> {
    override fun create(
        annotations: AnnotationList,
        lamp: Lamp<MinestomCommandActor?>
    ): CommandPermission<MinestomCommandActor>? {
        val permissionAnnotation =
            annotations.get(CommandPermissionAnnotation::class.java) ?: return null

        return MinestomCommandPermission(permissionAnnotation.permission)
    }


    private class MinestomCommandPermission(private val permission: String) : CommandPermission<MinestomCommandActor> {
        override fun isExecutableBy(actor: MinestomCommandActor): Boolean {
            if (actor.isConsole) return true
            val player = actor.asPlayer() as? LobbyPlayer ?: return false
            return player.hasPermission(permission)
        }

        override fun toString(): String {
            return "MinestomCommandPermission(permission=$permission)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MinestomCommandPermission) return false

            if (permission != other.permission) return false

            return true
        }

        override fun hashCode(): Int {
            return permission.hashCode()
        }
    }
}
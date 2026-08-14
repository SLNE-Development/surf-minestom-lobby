package dev.slne.minestom.lobby.server.command.permission

import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.command.AdditionalCommandPermissions
import dev.slne.minestom.lobby.api.command.RootCommandPermission
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
        val commandPermission = annotations.get(CommandPermissionAnnotation::class.java)
        val additionalPermissions = annotations.get(AdditionalCommandPermissions::class.java)
        val rootPermission = annotations.get(RootCommandPermission::class.java)
        if (commandPermission == null && additionalPermissions == null && rootPermission == null) {
            return null
        }

        return create(
            buildSet {
                if (commandPermission != null) {
                    add(commandPermission.permission)
                }
                if (additionalPermissions != null) {
                    addAll(additionalPermissions.permissions)
                }
                if (rootPermission != null) {
                    add(rootPermission.permission)
                }
            },
        )
    }

    private fun create(permissions: Set<String>): CommandPermission<MinestomCommandActor> =
        MinestomCommandPermission(permissions)

    private class MinestomCommandPermission(
        private val permissions: Set<String>,
    ) : CommandPermission<MinestomCommandActor> {
        override fun isExecutableBy(actor: MinestomCommandActor): Boolean {
            if (actor.isConsole) return true
            val player = actor.asPlayer() as? LobbyPlayer ?: return false
            return permissions.all(player::hasPermission)
        }

        override fun toString(): String {
            return "MinestomCommandPermission(permissions=$permissions)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MinestomCommandPermission) return false

            if (permissions != other.permissions) return false

            return true
        }

        override fun hashCode(): Int {
            return permissions.hashCode()
        }
    }
}

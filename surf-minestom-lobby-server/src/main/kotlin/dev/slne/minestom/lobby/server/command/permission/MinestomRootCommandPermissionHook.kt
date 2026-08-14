package dev.slne.minestom.lobby.server.command.permission

import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.command.RootCommandPermission
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import net.minestom.server.MinecraftServer
import net.minestom.server.command.ConsoleSender
import revxrsal.commands.command.ExecutableCommand
import revxrsal.commands.hook.CancelHandle
import revxrsal.commands.hook.CommandRegisteredHook
import revxrsal.commands.minestom.actor.MinestomCommandActor
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

@Singleton
class MinestomRootCommandPermissionHook : CommandRegisteredHook<MinestomCommandActor> {
    private val rootPermissions = ConcurrentHashMap<String, String>()

    override fun onRegistered(
        command: ExecutableCommand<MinestomCommandActor>,
        cancelHandle: CancelHandle,
    ) {
        val annotation = command.annotations().get(RootCommandPermission::class.java) ?: return

        require(command.size() == 1) {
            "@RootCommandPermission can only be used on a root command, but was used on '${command.path()}'"
        }

        val rootName = command.firstNode().name()
        val normalizedRootName = rootName.lowercase(Locale.ROOT)
        val previousPermission = rootPermissions.putIfAbsent(normalizedRootName, annotation.permission)
        require(previousPermission == null || previousPermission == annotation.permission) {
            "Command root '$rootName' declares conflicting root permissions: " +
                "'$previousPermission' and '${annotation.permission}'"
        }

        val minestomCommand = checkNotNull(MinecraftServer.getCommandManager().getCommand(rootName)) {
            "Minestom command '$rootName' was not registered before its root permission"
        }
        minestomCommand.setCondition { sender, _ ->
            when (sender) {
                is ConsoleSender -> true
                is LobbyPlayer -> sender.hasPermission(annotation.permission)
                else -> false
            }
        }
    }
}

package dev.slne.minestom.lobby.server.command.impl

import com.google.inject.Inject
import com.google.inject.Provider
import dev.slne.minestom.lobby.api.command.CommandPermission
import dev.slne.minestom.lobby.server.lifecycle.LobbyServerApplication
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.minestom.actor.MinestomCommandActor

@Command("stop")
@CommandPermission(LobbyPermissions.STOP_COMMAND)
class StopCommand @Inject constructor(
    private val lobbyServerApplication: Provider<LobbyServerApplication>,
) {

    @CommandPlaceholder
    fun stop(actor: MinestomCommandActor) {
        actor.reply(text("Der Server wird heruntergefahren...", NamedTextColor.GRAY))

        lobbyServerApplication.get().beginShutdown()
    }
}

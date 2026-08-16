package dev.slne.minestom.lobby.server.command.commandapi.brigadier

import com.google.inject.Inject
import com.google.inject.Singleton
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.minestom.lobby.api.command.commandapi.CommandAPI
import dev.slne.minestom.lobby.api.command.commandapi.executor.GENERIC_COMMAND_FAILURE
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.minestom.lobby.api.extension.addListener
import dev.slne.minestom.lobby.server.command.commandapi.MinestomCommandOwnership
import net.minestom.server.MinecraftServer
import net.minestom.server.command.CommandSender
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerPacketEvent
import net.minestom.server.network.packet.client.play.ClientCommandChatPacket
import net.minestom.server.network.packet.client.play.ClientSignedCommandChatPacket

/**
 * Routes command packets for CommandAPI-owned commands to Brigadier before Minestom parses them.
 *
 * A packet whose command root belongs to another registry is left alone, so commands registered
 * directly with Minestom keep working.
 */
@Singleton
class CommandPacketListener @Inject constructor(
    private val ownership: MinestomCommandOwnership,
) : EventRegistrar {

    override fun register(node: EventNode<Event>) {
        node.addListener(::handlePacket)
    }

    private fun handlePacket(event: PlayerPacketEvent) {
        val input = when (val packet = event.packet) {
            is ClientCommandChatPacket -> packet.message()
            is ClientSignedCommandChatPacket -> packet.message()
            else -> return
        }

        if (ownership.findInput(input) == null) return

        event.isCancelled = true
        dispatch(event.player, input)
    }

    companion object {
        /**
         * Runs [input] through Brigadier and reports a syntax failure to [sender] the way vanilla
         * does. Returns whether the command executed.
         */
        fun dispatch(sender: CommandSender, input: String): Boolean {
            val command = input.removePrefix("/")

            return try {
                CommandAPI.execute(sender, command)
                true
            } catch (failure: CommandSyntaxException) {
                reportSyntaxFailure(sender, command, failure)
                false
            } catch (failure: Throwable) {
                MinecraftServer.LOGGER.error("Command '{}' failed for {}", command, sender, failure)
                sender.sendMessage(GENERIC_COMMAND_FAILURE)
                false
            }
        }
    }
}

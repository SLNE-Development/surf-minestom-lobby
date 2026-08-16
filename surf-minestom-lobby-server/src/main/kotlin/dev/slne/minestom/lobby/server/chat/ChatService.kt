package dev.slne.minestom.lobby.server.chat

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.minestom.lobby.api.extension.CommandManager
import dev.slne.minestom.lobby.api.extension.PacketListenerManager
import dev.slne.minestom.lobby.api.extension.addListener
import dev.slne.minestom.lobby.server.chat.signature.RemoteChatSession
import dev.slne.minestom.lobby.server.command.commandapi.MinestomCommandOwnership
import dev.slne.minestom.lobby.server.command.commandapi.brigadier.CommandPacketListener
import dev.slne.minestom.lobby.server.lifecycle.LobbyService
import dev.slne.minestom.lobby.server.player.LobbyPlayerImpl
import dev.slne.minestom.lobby.server.player.requireLobbyPlayerImpl
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerTickEvent
import net.minestom.server.network.packet.client.play.*
import net.minestom.server.utils.PacketSendingUtils

/**
 * Sets up secure chat and handles chat processing.
 */
@Singleton
class ChatService @Inject constructor(
    private val ownership: MinestomCommandOwnership,
) : LobbyService, EventRegistrar {

    override suspend fun start() {
        LobbyChatTypes.register()
        ChatTranslations.register()
        registerPacketListeners()
    }

    override fun register(node: EventNode<Event>) {
        node.addListener(::handlePlayerTick)
        node.addListener(::handlePlayerDisconnect)
    }

    private fun registerPacketListeners() = with(PacketListenerManager) {
        setPlayListener(ClientChatMessagePacket::class.java) { packet, player ->
            player.requireLobbyPlayerImpl().chatHandler.handleChat(packet) { message ->
                ChatProcessor(player, message).process()
            }
        }

        setPlayListener(ClientChatAckPacket::class.java) { packet, player ->
            player.requireLobbyPlayerImpl().chatHandler.handleChatAck(packet)
        }

        setPlayListener(ClientChatSessionUpdatePacket::class.java) { packet, player ->
            player.requireLobbyPlayerImpl().chatHandler.handleChatSessionUpdate(packet) { session ->
                broadcastChatSession(player, session)
            }
        }

        setPlayListener(ClientCommandChatPacket::class.java) { packet, player ->
            player.requireLobbyPlayerImpl().chatHandler.handleUnsignedCommandChat(packet.message()) { command ->
                runCommand(player, command)
            }
        }

        setPlayListener(ClientSignedCommandChatPacket::class.java) { packet, player ->
            player.requireLobbyPlayerImpl().chatHandler.handleSignedCommandChat(packet) { command ->
                runCommand(player, command)
            }
        }
    }

    /**
     * Runs a command typed by [player], through Brigadier when the command belongs to the CommandAPI
     * and through Minestom otherwise.
     */
    private fun runCommand(player: LobbyPlayerImpl, command: String) {
        if (ownership.ownsInput(command)) {
            CommandPacketListener.dispatch(player, command)
        } else {
            CommandManager.execute(player, command)
        }
    }

    private fun broadcastChatSession(player: LobbyPlayerImpl, session: RemoteChatSession) {
        PacketSendingUtils.broadcastPlayPacket(player.chatSessionInfoPacket(session.asData()))
    }

    private fun handlePlayerTick(event: PlayerTickEvent) {
        event.player.requireLobbyPlayerImpl().chatHandler.tick()
    }

    private fun handlePlayerDisconnect(event: PlayerDisconnectEvent) {
        event.player.requireLobbyPlayerImpl().chatHandler.close()
    }
}

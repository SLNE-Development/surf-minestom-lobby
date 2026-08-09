package dev.slne.minestom.lobby.server.player.chat

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.extension.CommandManager
import dev.slne.minestom.lobby.api.extension.PacketListenerManager
import dev.slne.minestom.lobby.api.extension.addListener
import dev.slne.minestom.lobby.server.player.LobbyPlayerImpl
import dev.slne.minestom.lobby.server.util.requireLobbyPlayerImpl
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerTickEvent
import net.minestom.server.network.packet.client.play.*
import net.minestom.server.utils.PacketSendingUtils

@Singleton
class ChatService @Inject constructor() {

    fun initialize(eventNode: EventNode<Event>) {
        LobbyChatTypes.register()
        ChatTranslations.register()
        registerPacketListeners()

        eventNode.addListener(::handlePlayerTick)
        eventNode.addListener(::handlePlayerDisconnect)
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
                CommandManager.execute(player, command)
            }
        }

        setPlayListener(ClientSignedCommandChatPacket::class.java) { packet, player ->
            player.requireLobbyPlayerImpl().chatHandler.handleSignedCommandChat(packet) { command ->
                CommandManager.execute(player, command)
            }
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

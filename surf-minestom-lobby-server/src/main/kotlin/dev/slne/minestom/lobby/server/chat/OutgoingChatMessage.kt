package dev.slne.minestom.lobby.server.chat

import dev.slne.minestom.lobby.server.chat.signature.PlayerChatMessage
import dev.slne.minestom.lobby.server.player.LobbyPlayerImpl
import net.kyori.adventure.text.Component


sealed interface OutgoingChatMessage {

    val content: Component

    fun sendToPlayer(
        player: LobbyPlayerImpl,
        filtered: Boolean,
        chatType: BoundChatType,
        unsigned: Component? = null
    )


    data class Disguised(override val content: Component) : OutgoingChatMessage {
        override fun sendToPlayer(
            player: LobbyPlayerImpl,
            filtered: Boolean,
            chatType: BoundChatType,
            unsigned: Component?
        ) {
            player.chatHandler.sendDisguisedChatMessage(unsigned ?: content, chatType)
        }
    }


    data class Signed(val message: PlayerChatMessage) : OutgoingChatMessage {
        override val content: Component get() = message.decoratedContent()

        override fun sendToPlayer(
            player: LobbyPlayerImpl,
            filtered: Boolean,
            chatType: BoundChatType,
            unsigned: Component?
        ) {
            var filteredMessage = message.filter(filtered)
            if (unsigned != null) {
                filteredMessage = filteredMessage.withUnsignedContent(unsigned)
            }

            if (!filteredMessage.isFullyFiltered()) {
                player.chatHandler.sendPlayerChatMessage(filteredMessage, chatType)
            }
        }
    }

    companion object {
        fun create(message: PlayerChatMessage): OutgoingChatMessage =
            if (message.isSystem()) Disguised(message.decoratedContent()) else Signed(message)
    }
}

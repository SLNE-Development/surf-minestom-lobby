package dev.slne.minestom.lobby.server.player.chat

import dev.slne.minestom.lobby.api.chat.AsyncChatEvent
import dev.slne.minestom.lobby.api.chat.ChatRenderer
import dev.slne.minestom.lobby.api.extension.ConnectionManager
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import dev.slne.minestom.lobby.server.player.LobbyPlayerImpl
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.audience.ForwardingAudience
import net.kyori.adventure.text.Component
import net.kyori.adventure.translation.GlobalTranslator
import net.minestom.server.adventure.MinestomAdventure
import net.minestom.server.adventure.audience.Audiences
import net.minestom.server.command.ConsoleSender
import net.minestom.server.message.ChatType

class ChatProcessor(
    private val player: LobbyPlayer,
    private val message: PlayerChatMessage,
) {
    private val originalMessage = message.decoratedContent()
    private val outgoing = OutgoingChatMessage.create(message)

    private var messageChanged = false
    private var formatChanged = false

    suspend fun process() {
        val players = ConnectionManager.onlinePlayers
        val viewers = ObjectLinkedOpenHashSet<Audience>(players.size + 1).apply {
            addAll(players)
            add(Audiences.console())
        }

        val renderer = ChatRenderer.defaultRenderer()
        val event = AsyncChatEvent(
            player = player,
            viewers = viewers,
            renderer = renderer,
            message = originalMessage,
            originalMessage = originalMessage,
            signedMessage = message.adventureView()
        )

        AsyncChatEvent.node.call(event)

        readModifications(event, renderer)
        complete(event)
    }

    private fun readModifications(event: AsyncChatEvent, originalRenderer: ChatRenderer) {
        messageChanged = event.message != originalMessage
        if (originalRenderer !== event.renderer) {
            formatChanged = true
        }
    }

    private suspend fun complete(event: AsyncChatEvent) {
        if (event.isCancelled) return

        val displayName = player.displayName()
        val message = event.message
        val renderer = event.renderer
        val viewers = event.viewers

        val useVanillaChatType = renderer is ChatRenderer.Default
        val chatType = BoundChatType(
            chatType = if (useVanillaChatType) ChatType.CHAT else LobbyChatTypes.raw,
            name = displayName
        )

        when {
            formatChanged -> if (renderer is ChatRenderer.ViewerUnaware) {
                val rendered = renderer.render(player, displayName, message)
                broadcast(viewers, chatType, sendConcurrent = false) { rendered }
            } else {
                broadcast(viewers, chatType, sendConcurrent = true) { viewer ->
                    renderer.render(player, displayName, message, viewer)
                }
            }

            messageChanged -> {
                val rendered = if (useVanillaChatType) {
                    message
                } else {
                    (renderer as ChatRenderer.ViewerUnaware).render(player, displayName, message)
                }

                broadcast(viewers, chatType, sendConcurrent = false) { rendered }
            }

            else -> broadcast(viewers, chatType, sendConcurrent = false, unsignedFor = null)
        }
    }

    private suspend fun broadcast(
        viewers: Set<Audience>,
        chatType: BoundChatType,
        sendConcurrent: Boolean,
        unsignedFor: (suspend (Audience) -> Component)?
    ) {
        if (viewers.isEmpty()) return
        if (viewers.size == 1) {
            sendTo(viewers.first(), chatType, unsignedFor?.invoke(viewers.first()))
        } else if (sendConcurrent) {
            supervisorScope {
                for (viewer in viewers) {
                    launch {
                        sendTo(viewer, chatType, unsignedFor?.invoke(viewer))
                    }
                }
            }
        } else {
            for (viewer in viewers) {
                sendTo(viewer, chatType, unsignedFor?.invoke(viewer))
            }
        }
    }

    private fun sendTo(viewer: Audience, chatType: BoundChatType, unsigned: Component?) {
        when (viewer) {
            is LobbyPlayerImpl -> outgoing.sendToPlayer(
                viewer,
                filtered = false,
                chatType,
                unsigned
            )

            // The console has no language files, so the decoration is resolved here rather than
            // handed over as a translation key that would flatten to an empty string.
            is ConsoleSender -> viewer.sendMessage(
                GlobalTranslator.render(
                    chatType.decorate(unsigned ?: outgoing.content),
                    MinestomAdventure.getDefaultLocale()
                )
            )

            is ForwardingAudience.Single -> sendTo(viewer.audience(), chatType, unsigned)

            else -> {
                val message =
                    if (unsigned == null) message else message.withUnsignedContent(unsigned)
                viewer.sendMessage(message.adventureView(), chatType.adventure())
            }
        }
    }
}

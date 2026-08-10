package dev.slne.minestom.lobby.server.chat

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.chat.AsyncChatEvent
import dev.slne.minestom.lobby.api.chat.ChatRenderer
import dev.slne.minestom.lobby.api.event.SuspendingEventNode
import dev.slne.minestom.lobby.server.lifecycle.LobbyService
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.adventure.audience.Audiences

/**
 * Temporary test format
 */
@Singleton
@Deprecated("Temporary test format") // TODO: 10.08.2026 16:02 - Remove this class when the new chat system is implemented
class ChatFormatService @Inject constructor() : LobbyService {

    private var registration: SuspendingEventNode.Registration<AsyncChatEvent>? = null

    override suspend fun start() {
        registration = AsyncChatEvent.addListener { event ->
            val signedMessage = event.signedMessage

            event.renderer = ChatRenderer { _, sourceDisplayName, message, _ ->
                text()
                    .append(
                        text()
                            .append(text("[X] ", NamedTextColor.RED))
                            .clickEvent(ClickEvent.callback {
                                Audiences.players().deleteMessage(signedMessage)
                            })
                    )
                    .append(sourceDisplayName)
                    .append(text(": ", NamedTextColor.GRAY))
                    .append(message)
                    .build()
            }
        }
    }

    override suspend fun stop() {
        registration?.close()
        registration = null
    }
}

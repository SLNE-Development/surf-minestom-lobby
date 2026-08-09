package dev.slne.minestom.lobby.server.player.chat

import dev.slne.minestom.lobby.api.extension.ChatTypeRegistry
import net.kyori.adventure.text.Component
import net.minestom.server.message.ChatType
import net.minestom.server.message.ChatTypeDecoration
import net.minestom.server.registry.RegistryKey
import net.kyori.adventure.chat.ChatType as AdventureChatType

data class BoundChatType(
    val chatType: RegistryKey<ChatType>,
    val name: Component,
    val targetName: Component? = null
) {
    /**
     * The value the chat packets carry for this chat type.
     *
     * Vanilla serializes the chat type as a `Holder<ChatType>` via `ByteBufCodecs.holder`, which
     * writes a registry reference as `id + 1` and reserves `0` for "a full chat type follows
     * inline". Minestom's packet records declare a plain `VAR_INT` and do not apply that offset, so
     * it has to be applied here.
     *
     * Sending the raw registry id is not an off-by-one you get away with: `minecraft:chat` is id 0,
     * so the client would read the message as an inline chat type definition and start parsing the
     * following component bytes as one.
     *
     * Note this is *not* how every registry field is encoded — `holderRegistry` (dimension type,
     * for instance) writes the plain id, which is why Minestom's raw `getId` is right there.
     */
    val id: Int
        get() {
            val registryId = ChatTypeRegistry.getId(chatType)
            check(registryId != -1) { "Chat type $chatType is not registered" }

            return registryId + 1
        }

    fun withTargetName(targetName: Component) = copy(targetName = targetName)

    fun adventure(): AdventureChatType.Bound =
        AdventureChatType.chatType(chatType.key()).bind(name, targetName)

    fun decorate(content: Component): Component {
        val decoration = ChatTypeRegistry.get(chatType)?.chat() ?: return content

        val arguments = decoration.parameters().map { parameter ->
            when (parameter) {
                ChatTypeDecoration.Parameter.SENDER -> name
                ChatTypeDecoration.Parameter.TARGET -> targetName ?: Component.empty()
                ChatTypeDecoration.Parameter.CONTENT -> content
            }
        }

        return Component.translatable(decoration.translationKey(), arguments)
            .style(decoration.style())
    }
}

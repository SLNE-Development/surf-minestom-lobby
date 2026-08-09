package dev.slne.minestom.lobby.server.player

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.luckperms.LuckPermsService
import dev.slne.minestom.lobby.server.packet.framed
import dev.slne.minestom.lobby.server.packet.server.play.DeleteChatPacketModern
import dev.slne.minestom.lobby.server.player.handler.PlayerChatHandler
import net.kyori.adventure.chat.SignedMessage
import net.minestom.server.crypto.ChatSession
import net.minestom.server.crypto.MessageSignature
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket
import net.minestom.server.network.player.GameProfile
import net.minestom.server.network.player.PlayerConnection

class LobbyPlayerImpl @AssistedInject constructor(
    @Assisted playerConnection: PlayerConnection,
    @Assisted gameProfile: GameProfile,
    private val luckPermsService: LuckPermsService,
    config: ServerConfig,
) : LobbyPlayer(playerConnection, gameProfile) {


    val chatHandler = PlayerChatHandler(this, config.chat)

    override fun hasPermission(permission: String): Boolean {
        return luckPermsService.hasPermission(uuid, permission)
    }

    override fun getAddPlayerToList(): PlayerInfoUpdatePacket {
        val packet = super.getAddPlayerToList()
        val session = chatHandler.chatSession?.asData() ?: return packet

        return PlayerInfoUpdatePacket(
            packet.actions(),
            packet.entries().map { it.withChatSession(session) }
        )
    }

    override fun deleteMessage(signature: SignedMessage.Signature) {
        val packed = MessageSignature.Packed(MessageSignature(signature.bytes()))
        sendPacket(DeleteChatPacketModern(packed).framed())
    }

    fun chatSessionInfoPacket(session: ChatSession): PlayerInfoUpdatePacket =
        PlayerInfoUpdatePacket(
            PlayerInfoUpdatePacket.Action.INITIALIZE_CHAT,
            super.getAddPlayerToList().entries().first().withChatSession(session)
        )
}

private fun PlayerInfoUpdatePacket.Entry.withChatSession(session: ChatSession) =
    PlayerInfoUpdatePacket.Entry(
        uuid(), username(), properties(), listed(), latency(), gameMode(),
        displayName(), session, listOrder(), displayHat()
    )
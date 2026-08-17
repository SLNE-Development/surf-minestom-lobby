package dev.slne.minestom.lobby.server.player

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import dev.slne.minestom.lobby.server.chat.BoundChatType
import dev.slne.minestom.lobby.server.chat.LobbyChatTypes
import dev.slne.minestom.lobby.server.chat.OutgoingChatMessage
import dev.slne.minestom.lobby.server.chat.PlayerChatHandler
import dev.slne.minestom.lobby.server.chat.signature.PlayerChatMessage
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.integration.luckperms.LuckPermsService
import dev.slne.minestom.lobby.server.packet.framed
import dev.slne.minestom.lobby.server.packet.server.play.DeleteChatPacketModern
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.permission.PermissionChecker
import net.kyori.adventure.pointer.Pointers
import net.kyori.adventure.pointer.PointersSupplier
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.TriState
import net.luckperms.api.util.Tristate
import net.minestom.server.crypto.ChatSession
import net.minestom.server.crypto.MessageSignature
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket
import net.minestom.server.network.player.GameProfile
import net.minestom.server.network.player.PlayerConnection

class LobbyPlayerImpl @AssistedInject constructor(
    @Assisted playerConnection: PlayerConnection,
    @Assisted gameProfile: GameProfile,
    private val luckPermsService: LuckPermsService,
    chatConfig: ServerConfig.ChatConfig,
) : LobbyPlayer(playerConnection, gameProfile) {

    companion object {
        private val POINTERS_SUPPLIER = PointersSupplier.builder<LobbyPlayerImpl>()
            .parent(PLAYER_POINTERS_SUPPLIER)
            .resolving(PermissionChecker.POINTER, LobbyPlayerImpl::permissionChecker)
            .build()
    }

    val chatHandler = PlayerChatHandler(this, chatConfig)

    private val permissionChecker = PermissionChecker { permission ->
        when (luckPermsService.hasPermission(uuid, permission)) {
            Tristate.TRUE -> TriState.TRUE
            Tristate.FALSE -> TriState.FALSE
            Tristate.UNDEFINED -> TriState.NOT_SET
        }
    }

    override fun hasPermission(permission: String): Boolean {
        return luckPermsService.hasPermission(uuid, permission).asBoolean()
    }

    override fun pointers(): Pointers {
        return POINTERS_SUPPLIER.view(this)
    }

    override fun getAddPlayerToList(): PlayerInfoUpdatePacket {
        val packet = super.getAddPlayerToList()
        val session = chatHandler.chatSession?.asData() ?: return packet

        return PlayerInfoUpdatePacket(
            packet.actions(),
            packet.entries().map { it.withChatSession(session) }
        )
    }

    override fun sendSignedMessage(
        message: SignedMessage,
        boundName: Component,
        unsignedContent: Component?,
    ) {
        val signed = (message as? PlayerChatMessage.AdventureView)?.playerChatMessage
        if (signed == null) {
            sendMessage(unsignedContent ?: Component.text(message.message()))
            return
        }

        OutgoingChatMessage.create(signed).sendToPlayer(
            player = this,
            filtered = false,
            chatType = BoundChatType(LobbyChatTypes.raw, boundName),
            unsigned = unsignedContent,
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

    public override fun refreshAbilities() {
        super.refreshAbilities()
    }
}

private fun PlayerInfoUpdatePacket.Entry.withChatSession(session: ChatSession) =
    PlayerInfoUpdatePacket.Entry(
        uuid(), username(), properties(), listed(), latency(), gameMode(),
        displayName(), session, listOrder(), displayHat()
    )
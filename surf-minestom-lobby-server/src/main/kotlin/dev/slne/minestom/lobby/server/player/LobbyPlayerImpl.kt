package dev.slne.minestom.lobby.server.player

import com.google.inject.assistedinject.Assisted
import com.google.inject.assistedinject.AssistedInject
import dev.slne.minestom.lobby.api.chat.RemoteChatSender
import dev.slne.minestom.lobby.api.chat.RemoteSignedMessage
import dev.slne.minestom.lobby.api.extension.ConnectionManager
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import dev.slne.minestom.lobby.server.chat.BoundChatType
import dev.slne.minestom.lobby.server.chat.LobbyChatTypes
import dev.slne.minestom.lobby.server.chat.OutgoingChatMessage
import dev.slne.minestom.lobby.server.chat.PlayerChatHandler
import dev.slne.minestom.lobby.server.chat.signature.FILTER_MASK_PASS_THROUGH
import dev.slne.minestom.lobby.server.chat.signature.LastSeenMessages
import dev.slne.minestom.lobby.server.chat.signature.PlayerChatMessage
import dev.slne.minestom.lobby.server.chat.signature.SignedMessageBody
import dev.slne.minestom.lobby.server.chat.signature.SignedMessageLink
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
import net.minestom.server.entity.GameMode
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket
import net.minestom.server.network.player.GameProfile
import net.minestom.server.network.player.PlayerConnection
import java.util.EnumSet

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

    override fun captureSignedMessage(
        message: SignedMessage,
        unsignedContent: Component?,
    ): RemoteSignedMessage? {
        val signed = (message as? PlayerChatMessage.AdventureView)?.playerChatMessage ?: return null

        return RemoteSignedMessage(
            sender = signed.link.sender,
            sessionId = signed.link.sessionId,
            index = signed.link.index,
            signature = signed.signature,
            content = signed.signedBody.content,
            timestamp = signed.signedBody.timeStamp,
            salt = signed.signedBody.salt,
            lastSeen = signed.signedBody.lastSeen.entries,
            unsignedContent = unsignedContent
        )
    }

    override fun chatSession(): ChatSession? = chatHandler.chatSession?.asData()

    override fun sendRemoteSignedMessage(
        sender: RemoteChatSender,
        message: RemoteSignedMessage,
        boundName: Component,
    ) {
        val signed = PlayerChatMessage(
            link = SignedMessageLink(message.index, message.sender, message.sessionId),
            signature = message.signature,
            signedBody = SignedMessageBody(
                message.content,
                message.timestamp,
                message.salt,
                LastSeenMessages(message.lastSeen)
            ),
            unsignedContent = message.unsignedContent,
            filterMask = FILTER_MASK_PASS_THROUGH
        )

        val announce = ConnectionManager.getOnlinePlayerByUuid(sender.uuid) == null
        if (announce) sendPacket(remoteSenderInfoPacket(sender))

        chatHandler.sendPlayerChatMessage(signed, BoundChatType(LobbyChatTypes.raw, boundName))

        if (announce) sendPacket(PlayerInfoRemovePacket(sender.uuid))
    }

    private fun remoteSenderInfoPacket(sender: RemoteChatSender): PlayerInfoUpdatePacket {
        val actions = EnumSet.of(PlayerInfoUpdatePacket.Action.ADD_PLAYER)
        if (sender.session != null) actions.add(PlayerInfoUpdatePacket.Action.INITIALIZE_CHAT)

        return PlayerInfoUpdatePacket(
            actions,
            PlayerInfoUpdatePacket.Entry(
                sender.uuid,
                sender.username,
                emptyList(),
                false,
                0,
                GameMode.ADVENTURE,
                null,
                sender.session,
                0,
                false
            )
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

    override fun setGameMode(gameMode: GameMode): Boolean {
        val previousGameMode = getGameMode()
        if (!super.setGameMode(gameMode)) return false

        completeGameModeSwitch(previousGameMode)
        return true
    }
}

private fun PlayerInfoUpdatePacket.Entry.withChatSession(session: ChatSession) =
    PlayerInfoUpdatePacket.Entry(
        uuid(), username(), properties(), listed(), latency(), gameMode(),
        displayName(), session, listOrder(), displayHat()
    )
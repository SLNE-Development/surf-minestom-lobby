package dev.slne.minestom.lobby.server.packet.server.play

import dev.slne.minestom.lobby.server.packet.OverridingPacket
import net.minestom.server.crypto.MessageSignature
import net.minestom.server.network.NetworkBuffer
import net.minestom.server.network.NetworkBufferTemplate
import net.minestom.server.network.packet.server.play.DeleteChatPacket

data class DeleteChatPacketModern(
    val messageSignature: MessageSignature.Packed
) : OverridingPacket {

    override val overrides get() = DeleteChatPacket::class.java

    override fun write(buffer: NetworkBuffer) = buffer.write(SERIALIZER, this)

    companion object {
        val SERIALIZER: NetworkBuffer.Type<DeleteChatPacketModern> = NetworkBufferTemplate.template(
            MessageSignature.Packed.SERIALIZER, DeleteChatPacketModern::messageSignature,
            ::DeleteChatPacketModern
        )
    }
}

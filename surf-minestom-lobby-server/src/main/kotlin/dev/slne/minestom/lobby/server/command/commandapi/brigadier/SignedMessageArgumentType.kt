package dev.slne.minestom.lobby.server.command.commandapi.brigadier

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import dev.slne.minestom.lobby.server.chat.signature.PlayerChatMessage
import dev.slne.minestom.lobby.server.command.commandapi.SignedCommandArguments
import dev.slne.minestom.lobby.server.util.NIL_UUID
import net.kyori.adventure.chat.SignedMessage
import net.minestom.server.command.CommandSender
import net.minestom.server.entity.Player
import java.util.*

/**
 * Reads the rest of the command line and pairs it with the signature its sender produced for it.
 */
internal class SignedMessageArgumentType(
    private val nodeName: String,
) : ArgumentType<SignedMessage> {

    override fun parse(reader: StringReader): SignedMessage = read(reader, sender = null)

    override fun <S> parse(reader: StringReader, source: S): SignedMessage =
        read(reader, source as? CommandSender)

    private fun read(reader: StringReader, sender: CommandSender?): SignedMessage {
        val content = reader.remaining
        reader.cursor = reader.totalLength

        val message = SignedCommandArguments.find(nodeName) ?: PlayerChatMessage.unsigned(
            sender.profileId(),
            content
        )

        return message.adventureView()
    }
}

private fun CommandSender?.profileId(): UUID = (this as? Player)?.uuid ?: NIL_UUID

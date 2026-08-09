package dev.slne.minestom.lobby.server.player.chat

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.minestom.server.crypto.MessageSignature


class MessageSignatureCache(capacity: Int = DEFAULT_CAPACITY) {

    private val entries = arrayOfNulls<MessageSignature>(capacity)

    fun pack(signature: MessageSignature): Int {
        for (index in entries.indices) {
            if (signature == entries[index]) return index
        }

        return NOT_FOUND
    }

    fun unpack(id: Int): MessageSignature? = entries[id]

    fun push(body: SignedMessageBody, signature: MessageSignature?) {
        val lastSeen = body.lastSeen.entries
        val queue = ArrayDeque<MessageSignature>(lastSeen.size + 1)
        queue.addAll(lastSeen)

        if (signature != null) {
            queue.addLast(signature)
        }

        push(queue)
    }

    private fun push(queue: ArrayDeque<MessageSignature>) {
        val newEntries = ObjectOpenHashSet(queue)

        var index = 0
        while (queue.isNotEmpty() && index < entries.size) {
            val previous = entries[index]
            entries[index] = queue.removeLast()

            if (previous != null && !newEntries.contains(previous)) {
                queue.addFirst(previous)
            }

            index++
        }
    }

    companion object {
        const val NOT_FOUND = -1
        private const val DEFAULT_CAPACITY = 128

        fun createDefault() = MessageSignatureCache(DEFAULT_CAPACITY)
    }
}

package dev.slne.minestom.lobby.server.player.chat

import net.minestom.server.crypto.MessageSignature


data class LastSeenTrackedEntry(
    val signature: MessageSignature,
    val pending: Boolean
) {
    fun acknowledge(): LastSeenTrackedEntry = if (!pending) this else copy(pending = false)
}

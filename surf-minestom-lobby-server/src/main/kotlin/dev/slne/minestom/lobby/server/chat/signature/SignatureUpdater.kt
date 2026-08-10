package dev.slne.minestom.lobby.server.chat.signature


fun interface SignatureUpdater {
    fun update(output: Output)

    fun interface Output {
        fun update(payload: ByteArray)
    }
}

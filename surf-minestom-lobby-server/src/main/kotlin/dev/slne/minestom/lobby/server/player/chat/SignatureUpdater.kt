package dev.slne.minestom.lobby.server.player.chat


fun interface SignatureUpdater {
    fun update(output: Output)

    fun interface Output {
        fun update(payload: ByteArray)
    }
}

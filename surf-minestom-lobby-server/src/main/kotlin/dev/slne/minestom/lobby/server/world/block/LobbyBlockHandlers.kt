package dev.slne.minestom.lobby.server.world.block

import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.BinaryTag
import net.minestom.server.MinecraftServer
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.Tag

object LobbyBlockHandlers {

    private val CLIENT_TAGS = mapOf(
        "minecraft:skull" to listOf("profile", "note_block_sound", "custom_name"),
        "minecraft:sign" to listOf("front_text", "back_text", "is_waxed"),
        "minecraft:hanging_sign" to listOf("front_text", "back_text", "is_waxed"),
        "minecraft:banner" to listOf("patterns", "CustomName"),
        "minecraft:decorated_pot" to listOf("sherds"),
    )

    fun register() {
        val blockManager = MinecraftServer.getBlockManager()

        for ((namespace, keys) in CLIENT_TAGS) {
            val blockKey = Key.key(namespace)
            val handler = PassthroughBlockHandler(blockKey, keys.map { Tag.NBT(it) })
            blockManager.registerHandler(namespace) { handler }
        }
    }
}

private class PassthroughBlockHandler(
    private val key: Key,
    private val tags: List<Tag<BinaryTag>>
) : BlockHandler {
    override fun getKey() = key
    override fun getBlockEntityTags(): Collection<Tag<*>> = tags
}

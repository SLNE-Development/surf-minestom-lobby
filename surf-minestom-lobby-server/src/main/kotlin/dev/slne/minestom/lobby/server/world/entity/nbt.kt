package dev.slne.minestom.lobby.server.world.entity

import net.kyori.adventure.nbt.*
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.codec.Codec
import net.minestom.server.codec.Transcoder
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.registry.RegistryTranscoder
import java.util.*


fun CompoundBinaryTag.compoundOrNull(key: String) = get(key) as? CompoundBinaryTag

fun CompoundBinaryTag.stringOrNull(key: String) = (get(key) as? StringBinaryTag)?.value()

fun CompoundBinaryTag.numberOrNull(key: String) = get(key) as? NumberBinaryTag

fun CompoundBinaryTag.intOrNull(key: String) = numberOrNull(key)?.intValue()

fun CompoundBinaryTag.floatOrNull(key: String) = numberOrNull(key)?.floatValue()

fun CompoundBinaryTag.byteOrNull(key: String) = numberOrNull(key)?.byteValue()

fun CompoundBinaryTag.booleanOrNull(key: String) = numberOrNull(key)?.let { it.intValue() != 0 }


fun CompoundBinaryTag.numberList(key: String): DoubleArray? {
    val list = get(key) as? ListBinaryTag ?: return null

    return DoubleArray(list.size()) {
        (list[it] as? NumberBinaryTag)?.doubleValue() ?: return null
    }
}

fun CompoundBinaryTag.vecOrNull(key: String): Vec? {
    val values = numberList(key)?.takeIf { it.size == 3 } ?: return null

    return Vec(values[0], values[1], values[2])
}

fun CompoundBinaryTag.quaternionOrNull(key: String): FloatArray? {
    val values = numberList(key)?.takeIf { it.size == 4 } ?: return null

    return FloatArray(4) { values[it].toFloat() }
}


fun CompoundBinaryTag.posOrNull(): Pos? {
    val pos = numberList("Pos")?.takeIf { it.size == 3 } ?: return null
    val rotation = numberList("Rotation")

    return Pos(
        pos[0], pos[1], pos[2],
        rotation?.getOrNull(0)?.toFloat() ?: 0f,
        rotation?.getOrNull(1)?.toFloat() ?: 0f
    )
}

fun CompoundBinaryTag.uuidOrNull(key: String = "UUID"): UUID? {
    val parts = getIntArray(key).takeIf { it.size == 4 } ?: return null

    return UUID(
        (parts[0].toLong() shl 32) or (parts[1].toLong() and 0xFFFFFFFFL),
        (parts[2].toLong() shl 32) or (parts[3].toLong() and 0xFFFFFFFFL)
    )
}

fun CompoundBinaryTag.componentOrNull(key: String): Component? {
    val tag: BinaryTag = get(key) ?: return null
    val transcoder = RegistryTranscoder(Transcoder.NBT, MinecraftServer.getRegistries())

    return Codec.COMPONENT.decode(transcoder, tag).orElse(null)
}

inline fun <reified E : Enum<E>> String.toEnumOrNull(): E? =
    enumValues<E>().firstOrNull { it.name.equals(this, ignoreCase = true) }

private fun DoubleArray.getOrNull(index: Int) = if (index in indices) this[index] else null

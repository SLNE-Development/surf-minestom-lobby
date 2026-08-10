package dev.slne.minestom.lobby.server.util

import com.google.common.primitives.Longs
import java.util.*

val NIL_UUID: UUID = UUID(0L, 0L)

fun UUID.toByteArray(): ByteArray =
    Longs.toByteArray(mostSignificantBits) + Longs.toByteArray(leastSignificantBits)

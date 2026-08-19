package dev.slne.minestom.lobby.server.database.world

import dev.slne.minestom.lobby.api.key.SurfKey
import org.komapper.annotation.KomapperColumn
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable

@KomapperEntity
@KomapperTable("lobby_world")
data class LobbyWorldEntity(
    @KomapperId
    @KomapperColumn(name = "world_key", length = 64)
    val key: String,

    @KomapperColumn(length = 64)
    val sha256: String,

    @KomapperColumn("world_data")
    val data: ByteArray,
) {

    fun surfKey() = SurfKey.key(key)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LobbyWorldEntity) return false

        if (key != other.key) return false
        if (sha256 != other.sha256) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + sha256.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
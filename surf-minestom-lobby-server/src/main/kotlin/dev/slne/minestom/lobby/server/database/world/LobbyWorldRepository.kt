package dev.slne.minestom.lobby.server.database.world

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.database.LobbyDatabase
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.firstOrNull

@Singleton
class LobbyWorldRepository @Inject constructor(
    private val database: LobbyDatabase,
) {

    private val world = Meta.lobbyWorldEntity

    suspend fun find(key: String): LobbyWorldEntity? = database.query {
        runQuery {
            QueryDsl
                .from(world)
                .where {
                    world.key eq key
                }
                .firstOrNull()
        }
    }

    suspend fun findSha256(key: String): String? = database.query {
        runQuery {
            QueryDsl
                .from(world)
                .where {
                    world.key eq key
                }
                .select(world.sha256)
                .firstOrNull()
        }
    }

    suspend fun findAllKeys(): List<String> = database.query {
        runQuery {
            QueryDsl
                .from(world)
                .orderBy(world.key)
                .select(world.key)
        }
    }.filterNotNull()

    suspend fun upsert(entity: LobbyWorldEntity) {
        database.query {
            runQuery {
                QueryDsl
                    .insert(world)
                    .onDuplicateKeyUpdate()
                    .single(entity)
            }
        }
    }

    suspend fun delete(key: String): Boolean = database.query {
        runQuery {
            QueryDsl
                .delete(world)
                .where {
                    world.key eq key
                }
        }
    } > 0
}

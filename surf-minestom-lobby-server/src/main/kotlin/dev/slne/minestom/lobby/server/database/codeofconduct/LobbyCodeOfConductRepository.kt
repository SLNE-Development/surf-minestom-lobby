package dev.slne.minestom.lobby.server.database.codeofconduct

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.database.LobbyDatabase
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.firstOrNull

@Singleton
class LobbyCodeOfConductRepository @Inject constructor(
    private val database: LobbyDatabase,
) {

    private val conduct = Meta.lobbyCodeOfConductEntity

    suspend fun findAll(): List<LobbyCodeOfConductEntity> = database.query {
        runQuery {
            QueryDsl
                .from(conduct)
                .orderBy(conduct.localeKey)
        }
    }

    suspend fun findSha256(localeKey: String): String? = database.query {
        runQuery {
            QueryDsl
                .from(conduct)
                .where {
                    conduct.localeKey eq localeKey
                }
                .select(conduct.sha256)
                .firstOrNull()
        }
    }

    suspend fun upsert(entity: LobbyCodeOfConductEntity) {
        database.query {
            runQuery {
                QueryDsl
                    .insert(conduct)
                    .onDuplicateKeyUpdate()
                    .single(entity)
            }
        }
    }

    suspend fun delete(localeKey: String): Boolean = database.query {
        runQuery {
            QueryDsl
                .delete(conduct)
                .where {
                    conduct.localeKey eq localeKey
                }
        }
    } > 0
}

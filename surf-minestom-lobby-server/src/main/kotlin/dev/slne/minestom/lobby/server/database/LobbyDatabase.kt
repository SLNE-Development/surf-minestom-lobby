package dev.slne.minestom.lobby.server.database

import com.google.inject.Inject
import com.google.inject.Singleton
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.database.codeofconduct.lobbyCodeOfConductEntity
import dev.slne.minestom.lobby.server.database.world.lobbyWorldEntity
import dev.slne.minestom.lobby.server.lifecycle.LobbyService
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.dialect.mariadb.jdbc.MariaDbJdbcDialect
import org.komapper.dialect.postgresql.jdbc.PostgreSqlJdbcDialect
import org.komapper.jdbc.JdbcByteArrayType
import org.komapper.jdbc.JdbcDatabase
import org.komapper.jdbc.JdbcDataTypeProvider
import java.util.concurrent.Executors

@Singleton
class LobbyDatabase @Inject constructor(
    private val config: ServerConfig.DatabaseConfig,
) : LobbyService {
    companion object {
        private val LOGGER = ComponentLogger.logger()
        val SCHEMA_NAME_PATTERN = Regex("[A-Za-z_][A-Za-z0-9_]*")

        /** MariaDB maps [ByteArray] to `varbinary(500)`, which no Polar world fits into. */
        internal val MARIADB_DATA_TYPES = JdbcDataTypeProvider(JdbcByteArrayType("longblob"))
    }

    private lateinit var dispatcher: ExecutorCoroutineDispatcher
    private lateinit var dataSource: HikariDataSource
    private lateinit var database: JdbcDatabase

    override suspend fun start() {
        dispatcher = Executors
            .newVirtualThreadPerTaskExecutor()
            .asCoroutineDispatcher()

        try {
            withContext(dispatcher) {
                dataSource = HikariDataSource(
                    HikariConfig().apply {
                        poolName = "surf-minestom-lobby-database"

                        jdbcUrl = config.url
                        username = config.username
                        password = config.password

                        driverClassName = when (config.type) {
                            ServerConfig.DatabaseType.MARIADB -> "org.mariadb.jdbc.Driver"
                            ServerConfig.DatabaseType.POSTGRESQL -> "org.postgresql.Driver"
                        }

                        if (config.type == ServerConfig.DatabaseType.POSTGRESQL) {
                            schema = config.schema
                        }

                        maximumPoolSize = config.pool.maximumSize
                        minimumIdle = config.pool.minimumIdle.coerceAtMost(config.pool.maximumSize)
                        connectionTimeout = config.pool.connectionTimeoutMillis
                        validationTimeout = config.pool.validationTimeoutMillis
                    }
                )

                if (config.type == ServerConfig.DatabaseType.POSTGRESQL) {
                    createPostgresSchema()
                }

                database = when (config.type) {
                    ServerConfig.DatabaseType.MARIADB -> JdbcDatabase(
                        dataSource = dataSource,
                        dialect = MariaDbJdbcDialect(),
                        dataTypeProvider = MARIADB_DATA_TYPES,
                    )

                    ServerConfig.DatabaseType.POSTGRESQL -> JdbcDatabase(
                        dataSource = dataSource,
                        dialect = PostgreSqlJdbcDialect(),
                    )
                }

                database.runQuery {
                    QueryDsl.create(Meta.lobbyWorldEntity)
                }

                database.runQuery {
                    QueryDsl.create(Meta.lobbyCodeOfConductEntity)
                }

                dataSource.connection.use { connection ->
                    check(connection.isValid(5)) {
                        "Database connection validation failed"
                    }
                }
            }
        } catch (failure: Throwable) {
            if (::dataSource.isInitialized) {
                try {
                    dataSource.close()
                } catch (rollbackFailure: Throwable) {
                    failure.addSuppressed(rollbackFailure)
                }
            }

            try {
                dispatcher.close()
            } catch (rollbackFailure: Throwable) {
                failure.addSuppressed(rollbackFailure)
            }

            LOGGER.error("Failed to start LobbyDatabase.", failure)

            throw failure
        }
    }

    private fun createPostgresSchema() {
        require(SCHEMA_NAME_PATTERN.matches(config.schema)) {
            "Invalid PostgreSQL schema name '${config.schema}'"
        }

        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("CREATE SCHEMA IF NOT EXISTS ${config.schema}")
            }
        }
    }

    suspend fun <T> query(block: JdbcDatabase.() -> T): T {
        check(::database.isInitialized) { "LobbyDatabase has not been started yet" }
        return withContext(dispatcher) {
            database.block()
        }
    }

    @Suppress("ConvertTryFinallyToUseCall")
    override suspend fun stop() {
        if (!::dispatcher.isInitialized) {
            return
        }

        try {
            withContext(dispatcher) {
                if (::dataSource.isInitialized) {
                    dataSource.close()
                }
            }
        } finally {
            dispatcher.close()
        }
    }
}
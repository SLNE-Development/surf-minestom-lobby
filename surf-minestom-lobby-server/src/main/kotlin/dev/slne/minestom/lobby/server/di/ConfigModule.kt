package dev.slne.minestom.lobby.server.di

import com.google.inject.AbstractModule
import com.google.inject.Provides
import dev.slne.minestom.lobby.server.config.ServerConfig

class ConfigModule(private val config: ServerConfig) : AbstractModule() {

    override fun configure() {
        bind(ServerConfig::class.java).toInstance(config)
    }

    @Provides
    fun chatConfig(config: ServerConfig): ServerConfig.ChatConfig = config.chat

    @Provides
    fun sparkConfig(config: ServerConfig): ServerConfig.SparkConfig = config.performance.spark

    @Provides
    fun databaseConfig(config: ServerConfig): ServerConfig.DatabaseConfig = config.database

    @Provides
    fun worldConfig(config: ServerConfig): ServerConfig.WorldConfig = config.world
}

package dev.slne.minestom.lobby.config.auth

import dev.slne.minestom.lobby.config.serverConfig
import net.minestom.server.Auth

object AuthProvider {

    fun load(): Auth {
        val config = serverConfig.velocity

        if (!config.enabled) {
            return Auth.Online()
        }

        return Auth.Velocity(config.secret)
    }
}
package dev.slne.minestom.lobby.server.auth

import dev.slne.minestom.lobby.server.config.ServerConfig
import net.minestom.server.Auth

fun ServerConfig.createAuth(): Auth {
    return if (velocity.enabled) {
        Auth.Velocity(velocity.secret)
    } else {
        Auth.Online()
    }
}
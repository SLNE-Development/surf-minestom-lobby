package dev.slne.minestom.lobby.server.di

import com.google.inject.AbstractModule
import dev.slne.minestom.lobby.server.player.LobbyPlayerListener

class PlayerModule : AbstractModule() {

    override fun configure() {
        binder().bindEventRegistrar<LobbyPlayerListener>()
    }
}

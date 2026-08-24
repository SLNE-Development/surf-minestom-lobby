package dev.slne.minestom.lobby.server.di

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder
import dev.slne.minestom.lobby.api.di.bindEventRegistrar
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import dev.slne.minestom.lobby.api.player.PlayerLimit
import dev.slne.minestom.lobby.server.player.LobbyPlayerFactory
import dev.slne.minestom.lobby.server.player.LobbyPlayerImpl
import dev.slne.minestom.lobby.server.player.LobbyPlayerListener
import dev.slne.minestom.lobby.server.player.PlayerLimitService

class PlayerModule : AbstractModule() {

    override fun configure() {
        install(
            FactoryModuleBuilder()
                .implement(LobbyPlayer::class.java, LobbyPlayerImpl::class.java)
                .build(LobbyPlayerFactory::class.java)
        )

        bind(PlayerLimit::class.java).to(PlayerLimitService::class.java)

        binder().bindEventRegistrar<LobbyPlayerListener>()
        binder().bindEventRegistrar<PlayerLimitService>()
    }
}

package dev.slne.minestom.lobby.server.di

import com.google.inject.AbstractModule
import com.google.inject.Provides
import dev.slne.minestom.lobby.api.di.bindEventRegistrar
import dev.slne.minestom.lobby.api.instance.LobbyInstance
import dev.slne.minestom.lobby.server.world.LobbyWorldService
import dev.slne.minestom.lobby.server.world.entity.NoGravityListener
import net.minestom.server.instance.InstanceContainer

class WorldModule : AbstractModule() {

    override fun configure() {
        binder().bindEventRegistrar<NoGravityListener>()
    }

    @Provides
    @LobbyInstance
    fun lobbyInstance(world: LobbyWorldService): InstanceContainer = world.instance
}

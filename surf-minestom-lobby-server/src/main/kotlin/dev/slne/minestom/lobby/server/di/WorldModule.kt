package dev.slne.minestom.lobby.server.di

import com.google.inject.AbstractModule
import dev.slne.minestom.lobby.server.world.entity.NoGravityListener

class WorldModule : AbstractModule() {

    override fun configure() {
        binder().bindEventRegistrar<NoGravityListener>()
    }
}

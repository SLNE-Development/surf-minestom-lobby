package dev.slne.minestom.lobby.server.di

import com.google.inject.AbstractModule
import dev.slne.minestom.lobby.api.di.bindEventRegistrar
import dev.slne.minestom.lobby.server.permission.PermissionLevelService

class PermissionModule : AbstractModule() {

    override fun configure() {
        binder().bindEventRegistrar<PermissionLevelService>()
    }
}

package dev.slne.minestom.lobby.server.di

import com.google.inject.AbstractModule
import dev.slne.minestom.lobby.api.di.bindCommandRegistrar
import dev.slne.minestom.lobby.server.command.DefaultCommandRegistrar

class CommandModule : AbstractModule() {

    override fun configure() {
        binder().bindCommandRegistrar<DefaultCommandRegistrar>()
    }
}

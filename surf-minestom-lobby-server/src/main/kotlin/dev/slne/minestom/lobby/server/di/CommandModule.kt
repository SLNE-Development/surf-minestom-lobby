package dev.slne.minestom.lobby.server.di

import com.google.inject.AbstractModule
import dev.slne.minestom.lobby.api.di.bindCommandRegistrar
import dev.slne.minestom.lobby.api.di.bindEventRegistrar
import dev.slne.minestom.lobby.server.command.DefaultCommandRegistrar
import dev.slne.minestom.lobby.server.command.commandapi.MinestomSuggestionListener

class CommandModule : AbstractModule() {

    override fun configure() {
        binder().bindCommandRegistrar<DefaultCommandRegistrar>()
        binder().bindEventRegistrar<MinestomSuggestionListener>()
    }
}

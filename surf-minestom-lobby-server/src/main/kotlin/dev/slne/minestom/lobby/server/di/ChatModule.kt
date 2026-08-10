package dev.slne.minestom.lobby.server.di

import com.google.inject.AbstractModule
import dev.slne.minestom.lobby.api.di.bindEventRegistrar
import dev.slne.minestom.lobby.server.chat.ChatService

class ChatModule : AbstractModule() {

    override fun configure() {
        binder().bindEventRegistrar<ChatService>()
    }
}

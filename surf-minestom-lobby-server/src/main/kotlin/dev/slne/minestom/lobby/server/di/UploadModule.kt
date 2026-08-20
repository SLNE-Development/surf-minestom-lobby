package dev.slne.minestom.lobby.server.di

import com.google.inject.AbstractModule
import dev.slne.minestom.lobby.api.di.bindIntoSet
import dev.slne.minestom.lobby.server.codeofconduct.CodeOfConductService
import dev.slne.minestom.lobby.server.upload.UploadHandler
import dev.slne.minestom.lobby.server.world.LobbyWorldUploadHandler

class UploadModule : AbstractModule() {

    override fun configure() {
        binder().bindIntoSet<UploadHandler, LobbyWorldUploadHandler>()
        binder().bindIntoSet<UploadHandler, CodeOfConductService>()
    }
}

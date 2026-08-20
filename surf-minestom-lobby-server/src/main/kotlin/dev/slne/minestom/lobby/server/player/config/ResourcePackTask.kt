package dev.slne.minestom.lobby.server.player.config

import com.google.inject.Singleton

/** Waits for the resource packs Minestom queued for this player. */
@Singleton
class ResourcePackTask : ConfigurationTask {

    @Suppress("UnstableApiUsage")
    override fun run(context: ConfigurationContext) {
        context.player.resourcePackFuture?.join()
    }
}

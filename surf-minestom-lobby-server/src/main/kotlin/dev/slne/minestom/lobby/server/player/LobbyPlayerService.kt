package dev.slne.minestom.lobby.server.player

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.extension.ConnectionManager
import dev.slne.minestom.lobby.server.lifecycle.LobbyService
import dev.slne.minestom.lobby.server.player.config.*

@Singleton
class LobbyPlayerService @Inject constructor(
    private val playerFactory: LobbyPlayerFactory,
    private val loginGate: PlayerLoginGate,
    private val enabledFeatures: EnabledFeaturesTask,
    private val synchronizeRegistries: SynchronizeRegistriesTask,
    private val awaitSettings: AwaitSettingsTask,
    private val codeOfConduct: CodeOfConductConfigurationTask,
    private val resourcePack: ResourcePackTask,
    private val joinWorld: JoinWorldTask,
) : LobbyService {

    override suspend fun start() {
        ConnectionManager.setPlayerProvider(playerFactory::create)

        LobbyConfiguration.install(
            listOf(
                enabledFeatures,
                synchronizeRegistries,
                awaitSettings,
                codeOfConduct,
                resourcePack,
                joinWorld,
            ),
            loginGate,
        )
    }
}

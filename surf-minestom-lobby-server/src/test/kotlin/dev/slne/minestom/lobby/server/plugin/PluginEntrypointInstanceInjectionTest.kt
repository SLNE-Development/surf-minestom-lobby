package dev.slne.minestom.lobby.server.plugin

import com.google.inject.AbstractModule
import com.google.inject.CreationException
import com.google.inject.Guice
import com.google.inject.Inject
import com.google.inject.Provider
import com.google.inject.ProvisionException
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.Stage
import dev.slne.minestom.lobby.api.instance.LobbyInstance
import dev.slne.minestom.lobby.api.plugin.MinestomPlugin
import dev.slne.minestom.lobby.api.plugin.MinestomPluginEntrypoint
import dev.slne.minestom.lobby.api.plugin.annotation.DataDirectory
import dev.slne.minestom.lobby.api.plugin.annotation.MinestomPluginMeta
import net.minestom.server.MinecraftServer
import net.minestom.server.instance.InstanceContainer
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.nio.file.Path

/**
 * The server builds its injector in [Stage.PRODUCTION], so every singleton - a plugin's entrypoint
 * among them - is constructed while the injector is created, which is before the lobby world
 * exists. A plugin therefore has to ask for a [Provider] of the world rather than the world itself.
 *
 * These tests pin that down from both sides, because the difference only ever shows at runtime.
 */
@EnvTest
class PluginEntrypointInstanceInjectionTest {

    @Test
    fun `an entrypoint asking for the lobby world itself cannot be built`() {
        val failure = assertThrows(CreationException::class.java) {
            createInjector(EagerWorldPlugin())
        }

        assertTrue(
            failure.message?.contains(WORLD_MISSING) == true,
            "expected the world service's own complaint, got: ${failure.message}"
        )
    }

    @Test
    fun `an entrypoint asking for a provider of the lobby world is built`() {
        assertDoesNotThrow {
            createInjector(DeferredWorldPlugin())
        }
    }

    @Test
    fun `a deferred entrypoint reads the world once it exists`(env: Env) {
        val world = WorldHolder()
        val injector = createInjector(DeferredWorldPlugin(), world)
        val entrypoint = injector.getInstance(DeferredWorldEntrypoint::class.java)

        assertThrows(ProvisionException::class.java) { entrypoint.world() }

        val instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        world.container = instance

        try {
            assertDoesNotThrow { entrypoint.world() }
        } finally {
            env.destroyInstance(instance)
        }
    }

    private fun createInjector(
        plugin: MinestomPlugin,
        world: WorldHolder = WorldHolder(),
    ) = Guice.createInjector(
        Stage.PRODUCTION,
        WorldStub(world),
        PluginModule(plugin, Path.of("build", "tmp", "plugin-injection-test")),
    )

    /** Stands in for the server's world module, which only holds a world after it started one. */
    internal class WorldStub(private val world: WorldHolder) : AbstractModule() {
        @Provides
        @LobbyInstance
        fun lobbyInstance(): InstanceContainer =
            checkNotNull(world.container) { "$WORLD_MISSING - LobbyWorldService.start() has to run first" }
    }

    internal class WorldHolder {
        var container: InstanceContainer? = null
    }

    @MinestomPluginMeta("eager-world-plugin")
    internal class EagerWorldPlugin : MinestomPlugin(EagerWorldEntrypoint::class.java)

    @Singleton
    internal class EagerWorldEntrypoint @Inject constructor(
        @Suppress("unused") @DataDirectory private val dataDirectory: Path,
        @Suppress("unused") @LobbyInstance private val instance: InstanceContainer,
    ) : MinestomPluginEntrypoint {
        override suspend fun start() = Unit
    }

    @MinestomPluginMeta("deferred-world-plugin")
    internal class DeferredWorldPlugin : MinestomPlugin(DeferredWorldEntrypoint::class.java)

    @Singleton
    internal class DeferredWorldEntrypoint @Inject constructor(
        @Suppress("unused") @DataDirectory private val dataDirectory: Path,
        @LobbyInstance private val instance: Provider<InstanceContainer>,
    ) : MinestomPluginEntrypoint {
        override suspend fun start() = Unit

        fun world(): InstanceContainer = instance.get()
    }

    internal companion object {
        const val WORLD_MISSING = "The lobby world has not been created yet"
    }
}

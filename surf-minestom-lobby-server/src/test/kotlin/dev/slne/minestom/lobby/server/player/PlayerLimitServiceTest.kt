package dev.slne.minestom.lobby.server.player

import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.onVirtualThread
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.EventNode
import net.minestom.server.event.server.ServerListPingEvent
import net.minestom.server.ping.ServerListPingType
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@EnvTest
class PlayerLimitServiceTest {

    @Test
    fun `the server list reports the configured limit`(env: Env) {
        val service = install(env, maxPlayers = 42)

        val event = ServerListPingEvent(ServerListPingType.MODERN_FULL_RGB)
        onVirtualThread { EventDispatcher.call(event) }

        assertEquals(42, event.status.playerInfo()?.maxPlayers())
        assertEquals(service.playerCount, event.status.playerInfo()?.onlinePlayers())
    }

    @Test
    fun `the server list follows a limit changed at runtime`(env: Env) {
        install(env, maxPlayers = 42).maxPlayers = 7

        val event = ServerListPingEvent(ServerListPingType.MODERN_FULL_RGB)
        onVirtualThread { EventDispatcher.call(event) }

        assertEquals(7, event.status.playerInfo()?.maxPlayers())
    }

    @Test
    fun `a configured limit of zero is rejected`() {
        assertThrows<IllegalArgumentException> {
            PlayerLimitService(ServerConfig(maxPlayers = 0))
        }
    }

    @Test
    fun `a limit set to zero at runtime is rejected`() {
        val service = PlayerLimitService(ServerConfig())

        assertThrows<IllegalArgumentException> { service.maxPlayers = 0 }
        assertEquals(ServerConfig().maxPlayers, service.maxPlayers)
    }

    private fun install(env: Env, maxPlayers: Int): PlayerLimitService {
        val service = PlayerLimitService(ServerConfig(maxPlayers = maxPlayers))
        val node = EventNode.all("player-limit-test")

        service.register(node)
        env.process().eventHandler().addChild(node)

        return service
    }
}

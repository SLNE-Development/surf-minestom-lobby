package dev.slne.minestom.lobby.server.player

import dev.slne.minestom.lobby.api.chat.RemoteChatSender
import dev.slne.minestom.lobby.api.chat.RemoteSignedMessage
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import dev.slne.minestom.lobby.api.player.PlayerLimit
import dev.slne.minestom.lobby.api.player.event.PlayerLoginEvent
import dev.slne.minestom.lobby.server.onVirtualThread
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Pos
import net.minestom.server.crypto.ChatSession
import net.minestom.server.instance.Chunk
import net.minestom.server.network.packet.server.common.DisconnectPacket
import net.minestom.server.network.player.GameProfile
import net.minestom.server.network.player.PlayerConnection
import net.minestom.testing.Collector
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestConnection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EnvTest
class PlayerLoginGateTest {

    private lateinit var connection: TestConnection

    @Test
    fun `a player the server has room for is admitted`(env: Env) {
        val results = trackResults(env)
        val player = connectStubPlayer(env)

        assertTrue(onVirtualThread { gateFor(connected = 5, limit = 10).admit(player) })
        assertTrue(player.isOnline)
        assertEquals(listOf(PlayerLoginEvent.Result.ALLOWED), results)
    }

    @Test
    fun `the player taking the last slot is admitted`(env: Env) {
        val player = connectStubPlayer(env)

        assertTrue(onVirtualThread { gateFor(connected = 10, limit = 10).admit(player) })
        assertTrue(player.isOnline)
    }

    @Test
    fun `a player beyond the limit is kicked as full`(env: Env) {
        val results = trackResults(env)
        val player = connectStubPlayer(env)
        val disconnects = trackDisconnects()

        assertFalse(onVirtualThread { gateFor(connected = 11, limit = 10).admit(player) })
        assertFalse(player.isOnline)
        assertEquals(listOf(PlayerLoginEvent.Result.KICK_FULL), results)
        assertEquals(1, disconnects.collect().size)
    }

    @Test
    fun `a listener lets a player onto a full server`(env: Env) {
        env.process().eventHandler().addListener(PlayerLoginEvent::class.java) { it.allow() }

        val player = connectStubPlayer(env)
        val disconnects = trackDisconnects()

        assertTrue(onVirtualThread { gateFor(connected = 11, limit = 10).admit(player) })
        assertTrue(player.isOnline)
        assertTrue(disconnects.collect().isEmpty())
    }

    @Test
    fun `a refused player is disconnected with the message the listener set`(env: Env) {
        val reason = Component.text("Wartungsarbeiten")
        env.process().eventHandler().addListener(PlayerLoginEvent::class.java) {
            it.disallow(PlayerLoginEvent.Result.KICK_OTHER, reason)
        }

        val player = connectStubPlayer(env)
        val disconnects = trackDisconnects()

        assertFalse(onVirtualThread { gateFor(connected = 0, limit = 10).admit(player) })
        assertFalse(player.isOnline)
        assertEquals(listOf(reason), disconnects.collect().map(DisconnectPacket::message))
    }

    private fun trackResults(env: Env): List<PlayerLoginEvent.Result> {
        val results = mutableListOf<PlayerLoginEvent.Result>()
        env.process().eventHandler()
            .addListener(PlayerLoginEvent::class.java) { results += it.result }
        return results
    }

    private fun trackDisconnects(): Collector<DisconnectPacket> =
        connection.trackIncoming(DisconnectPacket::class.java)

    private fun gateFor(connected: Int, limit: Int) = PlayerLoginGate(
        object : PlayerLimit {
            override var maxPlayers = limit
            override val playerCount = connected
        }
    )

    private fun connectStubPlayer(env: Env): LobbyPlayer {
        env.process().connection().setPlayerProvider { playerConnection, gameProfile ->
            StubLobbyPlayer(playerConnection, gameProfile)
        }

        val instance = env.createFlatInstance()
        connection = env.createConnection()
        return onVirtualThread { connection.connect(instance, Pos(0.0, 42.0, 0.0)) } as LobbyPlayer
    }

    private class StubLobbyPlayer(
        playerConnection: PlayerConnection,
        gameProfile: GameProfile,
    ) : LobbyPlayer(playerConnection, gameProfile) {

        override fun hasPermission(permission: String) = false

        override fun sendSignedMessage(
            message: SignedMessage,
            boundName: Component,
            unsignedContent: Component?,
        ) = Unit

        override fun captureSignedMessage(
            message: SignedMessage,
            unsignedContent: Component?,
        ): RemoteSignedMessage? = null

        override fun chatSession(): ChatSession? = null

        override fun sendRemoteSignedMessage(
            sender: RemoteChatSender,
            message: RemoteSignedMessage,
            boundName: Component,
        ) = Unit

        override fun sendChunk(chunk: Chunk) = sendPacket(chunk.fullDataPacket)
    }
}

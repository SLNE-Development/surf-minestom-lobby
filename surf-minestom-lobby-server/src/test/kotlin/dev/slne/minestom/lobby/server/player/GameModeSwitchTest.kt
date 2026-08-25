package dev.slne.minestom.lobby.server.player

import dev.slne.minestom.lobby.server.onVirtualThread
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket
import net.minestom.server.network.player.GameProfile
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestConnection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.util.UUID

@EnvTest
class GameModeSwitchTest {

    @Test
    fun `a spectator is taken off every client that is not one`(env: Env) {
        val (spectator, other, otherConnection) = connectPair(env)
        val destroys = otherConnection.trackIncoming(DestroyEntitiesPacket::class.java)

        spectator.switchGameMode(GameMode.SPECTATOR)

        assertFalse(other in spectator.viewers, "a non-spectator was still shown the spectator")
        assertTrue(spectator in other.viewers, "the spectator stopped seeing a non-spectator")
        assertTrue(spectator.isInvisible, "the spectator was not marked invisible")
        assertEquals(
            listOf(listOf(spectator.entityId)),
            destroys.collect().map(DestroyEntitiesPacket::entityIds),
            "the spectator was not removed from the other client",
        )
    }

    @Test
    fun `spectators keep seeing each other`(env: Env) {
        val (spectator, other) = connectPair(env)

        spectator.switchGameMode(GameMode.SPECTATOR)
        other.switchGameMode(GameMode.SPECTATOR)

        assertTrue(other in spectator.viewers, "a spectator was not shown another spectator")
        assertTrue(spectator in other.viewers, "a spectator was not shown another spectator")
    }

    @Test
    fun `leaving spectator shows the player to everyone again`(env: Env) {
        val (spectator, other) = connectPair(env)

        spectator.switchGameMode(GameMode.SPECTATOR)
        spectator.switchGameMode(GameMode.ADVENTURE)

        assertTrue(other in spectator.viewers, "the player stayed hidden after leaving spectator")
        assertFalse(spectator.isInvisible, "the player stayed invisible after leaving spectator")
    }

    @Test
    fun `leaving spectator hides the spectators that were visible`(env: Env) {
        val (leaving, staying) = connectPair(env)

        leaving.switchGameMode(GameMode.SPECTATOR)
        staying.switchGameMode(GameMode.SPECTATOR)
        leaving.switchGameMode(GameMode.ADVENTURE)

        assertFalse(leaving in staying.viewers, "a spectator was still shown to a non-spectator")
        assertTrue(staying in leaving.viewers, "the remaining spectator stopped seeing the player")
    }

    @Test
    fun `a switch between the other modes leaves visibility alone`(env: Env) {
        val (player, other) = connectPair(env)

        player.switchGameMode(GameMode.CREATIVE)

        assertTrue(other in player.viewers, "an ordinary mode switch hid the player")
        assertFalse(player.isInvisible, "an ordinary mode switch made the player invisible")
    }

    /** Applies [gameMode] the way [LobbyPlayerImpl] does. */
    private fun Player.switchGameMode(gameMode: GameMode) {
        val previousGameMode = this.gameMode

        assertTrue(setGameMode(gameMode), "the game mode change was refused")
        completeGameModeSwitch(previousGameMode)
    }

    private fun connectPair(env: Env): Triple<Player, Player, TestConnection> {
        val instance = env.createFlatInstance()
        instance.loadChunk(0, 0).join()

        val (first, _) = connect(env, instance, "First")
        val (second, secondConnection) = connect(env, instance, "Second")

        awaitSpawn(env, first, second)
        assertTrue(second in first.viewers, "the players did not see each other to begin with")
        assertTrue(first in second.viewers, "the players did not see each other to begin with")

        return Triple(first, second, secondConnection)
    }

    /** Waits for the spawn, which the harness completes off the test thread. */
    private fun awaitSpawn(env: Env, vararg players: Player) {
        repeat(SPAWN_ATTEMPTS) {
            if (players.all { player -> player.instance != null }) return
            env.tick()
            Thread.sleep(SPAWN_POLL_MILLIS)
        }

        fail<Nothing>("the players never spawned")
    }

    private fun connect(
        env: Env,
        instance: Instance,
        username: String,
    ): Pair<Player, TestConnection> {
        val connection = env.createConnection(GameProfile(UUID.randomUUID(), username))
        val player = onVirtualThread { connection.connect(instance, Pos(0.0, 42.0, 0.0)) }
        return player to connection
    }

    private companion object {
        const val SPAWN_ATTEMPTS = 200
        const val SPAWN_POLL_MILLIS = 5L
    }
}

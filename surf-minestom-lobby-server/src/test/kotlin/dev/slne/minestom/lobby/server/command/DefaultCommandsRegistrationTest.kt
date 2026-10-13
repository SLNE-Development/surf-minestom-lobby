package dev.slne.minestom.lobby.server.command

import com.google.inject.Provider
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.minestom.lobby.server.command.impl.*
import dev.slne.minestom.lobby.server.version.LobbyVersionService
import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.permission.PermissionProvider
import dev.slne.surf.api.minestom.server.SurfMinestomServerBuilder
import dev.slne.surf.api.minestom.server.surfMinestomServer
import net.kyori.adventure.util.TriState
import net.minestom.server.command.CommandManager
import net.minestom.server.command.CommandSender
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.network.player.GameProfile
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestConnection
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.CompletableFuture

private val DEFAULT_COMMAND_NAMES =
    listOf("gamemode", "kill", "list", "kick", "difficulty", "stop", "version")

@EnvTest
class DefaultCommandsRegistrationTest {
    @Test
    fun `every default command is registered`(env: Env) =
        withSurfApi(env) { manager ->
            registerAll()

            DEFAULT_COMMAND_NAMES.forEach { name ->
                assertTrue(manager.commandExists(name), "'$name' was not registered")
            }
            assertTrue(manager.commandExists("gm"), "the gamemode alias was not registered")
            assertTrue(manager.commandExists("ver"), "the version alias was not registered")
        }

    @Test
    fun `a command is hidden from a sender without its permission`(env: Env) =
        withSurfApi(env) { manager ->
            registerAll()

            val instance = env.createEmptyInstance()
            var player: Player? = null
            try {
                player = connect(env.createConnection(), instance)

                // An ordinary player holds no lobby permission, so every default command is
                // invisible to it and its input cannot resolve.
                DEFAULT_COMMAND_NAMES.forEach { name ->
                    assertFalse(
                        runCommand(player, name),
                        "'$name' was reachable without permission"
                    )
                }
                assertTrue(runCommand(manager.consoleSender, "list"), "the console was denied")
            } finally {
                player?.remove()
                env.destroyInstance(instance)
            }
        }

    @Test
    fun `player-only and branch executors dispatch for a permitted player`(env: Env) =
        withSurfApi(env, { permissions(PermissionProvider { _, _ -> TriState.TRUE }) }) { manager ->
            killCommand()
            gamemodeCommand()

            val instance = env.createEmptyInstance()
            var player: Player? = null
            try {
                player = connect(
                    env.createConnection(GameProfile(UUID.randomUUID(), "Tester")),
                    instance,
                )

                assertTrue(
                    runCommand(player, "kill"),
                    "the player-only root executor of 'kill' is unreachable",
                )
                assertTrue(
                    runCommand(player, "gamemode creative"),
                    "the player-only executor on a non-terminal argument node is unreachable",
                )
                // The command applies the mode on the next tick
                env.tick()
                assertEquals(GameMode.CREATIVE, player.gameMode)

                assertTrue(
                    runCommand(manager.consoleSender, "gamemode survival Tester"),
                    "the target branch of 'gamemode' is unreachable",
                )
                env.tick()
                assertEquals(GameMode.SURVIVAL, player.gameMode)
            } finally {
                player?.remove()
                env.destroyInstance(instance)
            }
        }

    private fun registerAll() {
        gamemodeCommand()
        killCommand()
        listPlayersCommand()
        kickCommand()
        difficultyCommand()
        stopCommand(Provider { error("The stop command must not run during registration") })
        versionCommand(LobbyVersionService())
    }

    /** Runs [input] as [sender] and reports whether a command executed. */
    private fun runCommand(sender: CommandSender, input: String): Boolean = try {
        CommandAPI.execute(sender, input)
        true
    } catch (_: CommandSyntaxException) {
        false
    }

    private fun withSurfApi(
        env: Env,
        configure: SurfMinestomServerBuilder.() -> Unit = {},
        block: (CommandManager) -> Unit,
    ) {
        val api = surfMinestomServer(configure)
        try {
            block(env.process().command())
        } finally {
            api.shutdown()
        }
    }

    private fun connect(connection: TestConnection, instance: Instance): Player {
        val player = CompletableFuture<Player>()
        Thread.startVirtualThread {
            runCatching { connection.connect(instance, Pos.ZERO) }
                .onSuccess(player::complete)
                .onFailure(player::completeExceptionally)
        }
        return player.join()
    }
}

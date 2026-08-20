package dev.slne.minestom.lobby.server.command

import com.google.inject.Provider
import dev.slne.minestom.lobby.api.chat.RemoteChatSender
import dev.slne.minestom.lobby.api.chat.RemoteSignedMessage
import dev.slne.minestom.lobby.api.command.commandapi.CommandAPI
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import dev.slne.minestom.lobby.server.command.commandapi.MinestomCommandAPIPlatform
import dev.slne.minestom.lobby.server.command.commandapi.MinestomCommandOwnership
import dev.slne.minestom.lobby.server.command.commandapi.runCommand
import dev.slne.minestom.lobby.server.command.impl.difficultyCommand
import dev.slne.minestom.lobby.server.command.impl.gamemodeCommand
import dev.slne.minestom.lobby.server.command.impl.kickCommand
import dev.slne.minestom.lobby.server.command.impl.killCommand
import dev.slne.minestom.lobby.server.command.impl.listPlayersCommand
import dev.slne.minestom.lobby.server.command.impl.stopCommand
import java.util.UUID
import java.util.concurrent.CompletableFuture
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import net.minestom.server.command.CommandManager
import net.minestom.server.coordinate.Pos
import net.minestom.server.crypto.ChatSession
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.network.player.GameProfile
import net.minestom.server.network.player.PlayerConnection
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestConnection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private val DEFAULT_COMMAND_NAMES = listOf("gamemode", "kill", "list", "kick", "difficulty", "stop")

@EnvTest
class DefaultCommandsRegistrationTest {
    @Test
    fun `every default command is claimed by the command api`(env: Env) =
        withPlatform(env) { _, ownership ->
            registerAll()

            DEFAULT_COMMAND_NAMES.forEach { name ->
                assertTrue(ownership.ownsInput(name), "'$name' was not registered")
            }
            assertTrue(ownership.ownsInput("gm"), "the gamemode alias was not registered")
        }

    @Test
    fun `a command is hidden from a sender without its permission`(env: Env) =
        withPlatform(env) { manager, _ ->
            registerAll()

            val instance = env.createEmptyInstance()
            var player: Player? = null
            try {
                player = connect(env.createConnection(), instance)

                // An ordinary player holds no lobby permission, so every default command is
                // invisible to it and its input cannot resolve.
                DEFAULT_COMMAND_NAMES.forEach { name ->
                    assertFalse(runCommand(player, name), "'$name' was reachable without permission")
                }
                assertTrue(runCommand(manager.consoleSender, "list"), "the console was denied")
            } finally {
                player?.remove()
                env.destroyInstance(instance)
            }
        }

    @Test
    fun `player-only and branch executors dispatch for a permitted player`(env: Env) {
        env.process().connection().setPlayerProvider(::PermittedLobbyPlayer)
        try {
            withPlatform(env) { manager, _ ->
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
        } finally {
            env.process().connection().setPlayerProvider(null)
        }
    }

    private fun registerAll() {
        gamemodeCommand()
        killCommand()
        listPlayersCommand()
        kickCommand()
        difficultyCommand()
        stopCommand(Provider { error("The stop command must not run during registration") })
    }

    private fun withPlatform(env: Env, block: (CommandManager, MinestomCommandOwnership) -> Unit) {
        val manager = env.process().command()
        val ownership = MinestomCommandOwnership()
        val platform = MinestomCommandAPIPlatform(manager, ownership)
        CommandAPI.installPlatform(platform)
        try {
            block(manager, ownership)
        } finally {
            platform.close()
            CommandAPI.uninstallPlatform(platform)
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

    private class PermittedLobbyPlayer(
        connection: PlayerConnection,
        profile: GameProfile,
    ) : LobbyPlayer(connection, profile) {
        override fun hasPermission(permission: String): Boolean = true

        override fun sendSignedMessage(
            message: SignedMessage,
            boundName: Component,
            unsignedContent: Component?,
        ) = sendMessage(unsignedContent ?: Component.text(message.message()))

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
    }
}

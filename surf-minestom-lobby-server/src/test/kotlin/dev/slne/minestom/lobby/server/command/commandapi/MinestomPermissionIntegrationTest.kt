package dev.slne.minestom.lobby.server.command.commandapi

import dev.slne.minestom.lobby.api.chat.RemoteChatSender
import dev.slne.minestom.lobby.api.chat.RemoteSignedMessage
import dev.slne.minestom.lobby.api.command.commandapi.CommandAPI
import dev.slne.minestom.lobby.api.command.commandapi.CommandAPICommand
import dev.slne.minestom.lobby.api.command.commandapi.argument.StringArgument
import dev.slne.minestom.lobby.api.command.commandapi.dsl.anyExecutor
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import net.minestom.server.command.builder.CommandResult
import net.minestom.server.coordinate.Pos
import net.minestom.server.crypto.ChatSession
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

@EnvTest
class MinestomPermissionIntegrationTest {
    @Test
    fun `root subcommand and argument requirements all gate execution`(env: Env) {
        withPlatform(env) { manager ->
            val executions = AtomicInteger()
            CommandAPICommand("requirements-ok")
                .withRequirement { true }
                .withSubcommand(
                    CommandAPICommand("child")
                        .withRequirement { true }
                        .withArguments(StringArgument("value").withRequirement { true })
                        .anyExecutor { _, _ -> executions.incrementAndGet() },
                )
                .register()
            CommandAPICommand("requirements-root-denied")
                .withRequirement { false }
                .anyExecutor { _, _ -> executions.incrementAndGet() }
                .register()
            CommandAPICommand("requirements-path-denied")
                .withSubcommand(
                    CommandAPICommand("child")
                        .withRequirement { false }
                        .anyExecutor { _, _ -> executions.incrementAndGet() },
                )
                .register()
            CommandAPICommand("requirements-argument-denied")
                .withArguments(StringArgument("value").withRequirement { false })
                .anyExecutor { _, _ -> executions.incrementAndGet() }
                .register()

            assertTrue(runCommand(manager.consoleSender, "requirements-ok child value"))
            assertFalse(runCommand(manager.consoleSender, "requirements-root-denied"))
            assertFalse(runCommand(manager.consoleSender, "requirements-path-denied child"))
            assertFalse(runCommand(manager.consoleSender, "requirements-argument-denied value"))
            assertEquals(1, executions.get())
        }
    }

    @Test
    fun `console bypasses all accumulated permissions`(env: Env) {
        withPlatform(env) { manager ->
            val executions = AtomicInteger()
            CommandAPICommand("console-permission")
                .withPermission("lobby.root")
                .withArguments(StringArgument("value").withPermission("lobby.value"))
                .anyExecutor { _, _ -> executions.incrementAndGet() }
                .register()

            assertTrue(runCommand(manager.consoleSender, "console-permission accepted"))
            assertEquals(1, executions.get())
        }
    }

    @Test
    fun `lobby player must have every accumulated permission`(env: Env) {
        val permissionsByName = mapOf(
            "Allowed" to setOf("lobby.root", "lobby.value"),
            "Denied" to setOf("lobby.root"),
        )
        env.process().connection().setPlayerProvider { connection, profile ->
            TestLobbyPlayer(connection, profile, permissionsByName[profile.name].orEmpty())
        }
        try {
            withPlatform(env) { manager ->
                val executions = AtomicInteger()
                CommandAPICommand("lobby-permission")
                    .withPermission("lobby.root")
                    .withArguments(StringArgument("value").withPermission("lobby.value"))
                    .anyExecutor { _, _ -> executions.incrementAndGet() }
                    .register()
                val instance = env.createEmptyInstance()
                val players = mutableListOf<Player>()
                try {
                    val allowed = connect(env.createConnection(profile("Allowed")), instance).also(players::add)
                    val denied = connect(env.createConnection(profile("Denied")), instance).also(players::add)

                    assertTrue(allowed is LobbyPlayer)
                    assertTrue(denied is LobbyPlayer)
                    assertTrue(runCommand(allowed, "lobby-permission accepted"))
                    assertFalse(runCommand(denied, "lobby-permission rejected"))
                    assertEquals(1, executions.get())
                } finally {
                    players.forEach(Player::remove)
                    env.destroyInstance(instance)
                }
            }
        } finally {
            env.process().connection().setPlayerProvider(null)
        }
    }

    @Test
    fun `ordinary Minestom player is denied when any permission is required`(env: Env) {
        withPlatform(env) { manager ->
            val executions = AtomicInteger()
            CommandAPICommand("ordinary-player-permission")
                .withPermission("lobby.root")
                .anyExecutor { _, _ -> executions.incrementAndGet() }
                .register()
            val instance = env.createEmptyInstance()
            var player: Player? = null
            try {
                player = connect(env.createConnection(), instance)

                assertFalse(runCommand(player, "ordinary-player-permission"))
                assertEquals(0, executions.get())
            } finally {
                player?.remove()
                env.destroyInstance(instance)
            }
        }
    }

    private inline fun withPlatform(env: Env, block: (net.minestom.server.command.CommandManager) -> Unit) {
        val manager = env.process().command()
        val platform = MinestomCommandAPIPlatform(manager, MinestomCommandOwnership())
        CommandAPI.installPlatform(platform)
        try {
            block(manager)
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

    private fun profile(name: String) = GameProfile(UUID.randomUUID(), name)

    private class TestLobbyPlayer(
        connection: PlayerConnection,
        profile: GameProfile,
        private val grantedPermissions: Set<String>,
    ) : LobbyPlayer(connection, profile) {
        override fun hasPermission(permission: String): Boolean = permission in grantedPermissions

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

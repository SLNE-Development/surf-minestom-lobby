package dev.slne.minestom.lobby.server.command.commandapi.brigadier

import dev.slne.minestom.lobby.api.command.commandapi.CommandAPI
import dev.slne.minestom.lobby.api.command.commandapi.dsl.anyExecutor
import dev.slne.minestom.lobby.api.command.commandapi.dsl.commandTree
import dev.slne.minestom.lobby.api.command.commandapi.dsl.literalArgument
import dev.slne.minestom.lobby.api.command.commandapi.dsl.stringArgument
import dev.slne.minestom.lobby.api.command.commandapi.executor.CommandArguments
import dev.slne.minestom.lobby.server.command.commandapi.MinestomCommandAPIPlatform
import dev.slne.minestom.lobby.server.command.commandapi.MinestomCommandOwnership
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference

@EnvTest
class SharedNodeNameTest {
    @Test
    fun `a literal and an argument share a name with no renaming`(env: Env) {
        val manager = env.process().command()
        val platform = MinestomCommandAPIPlatform(manager, MinestomCommandOwnership())
        CommandAPI.installPlatform(platform)
        try {
            val received = AtomicReference<CommandArguments>()
            commandTree("send") {
                literalArgument("server") {
                    stringArgument("server") { anyExecutor { _, args -> received.set(args) } }
                }
            }

            assertEquals(1, CommandAPI.execute(manager.consoleSender, "send server lobby-1"))
            assertEquals("lobby-1", received.get().get<String>("server"))
        } finally {
            platform.close()
            CommandAPI.uninstallPlatform(platform)
        }
    }
}

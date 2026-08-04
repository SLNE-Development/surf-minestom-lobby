package dev.slne.minestom.lobby.server.console

import net.minecrell.terminalconsole.SimpleTerminalConsole
import net.minestom.server.MinecraftServer

class LobbyTerminalConsole(
    private val shutdownHandler: () -> Unit
) : SimpleTerminalConsole() {
    override fun isRunning(): Boolean {
        return MinecraftServer.isStarted()
    }

    override fun runCommand(command: String) {
        val commandManager = MinecraftServer.getCommandManager()
        commandManager.execute(commandManager.consoleSender, command)
    }

    override fun shutdown() {
        shutdownHandler()
    }
}
package dev.slne.minestom.lobby.server.permission

import net.minestom.server.entity.GameMode

object LobbyPermissions {

    private const val PREFIX = "minestom.lobby"

    private const val COMMAND_PREFIX = "$PREFIX.command"

    const val GAMEMODE_COMMAND = "$COMMAND_PREFIX.gamemode"
    const val DIFFICULTY_COMMAND = "$COMMAND_PREFIX.difficulty"
    const val KILL_COMMAND = "$COMMAND_PREFIX.kill"
    const val LIST_COMMAND = "$COMMAND_PREFIX.list"
    const val STOP_COMMAND = "$COMMAND_PREFIX.stop"
    const val VERSION_COMMAND = "$COMMAND_PREFIX.version"
    const val KICK_COMMAND = "$COMMAND_PREFIX.kick"
    const val UPLOADS_COMMAND = "$COMMAND_PREFIX.uploads"

    const val MAX_OP_LEVEL = 4

    private const val OP_PREFIX = "$PREFIX.op"
    private const val GAMEMODE_SWITCHER_PREFIX = "$PREFIX.gamemode-switcher."

    fun opLevel(level: Int): String = "$OP_PREFIX.$level"

    fun gamemodeSwitcher(gameMode: GameMode): String =
        "$GAMEMODE_SWITCHER_PREFIX${gameMode.name.lowercase()}"
}
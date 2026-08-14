package dev.slne.minestom.lobby.api.command

import dev.slne.minestom.lobby.api.coroutine.minestomScope
import dev.slne.minestom.lobby.api.command.commandapi.executor.GENERIC_COMMAND_FAILURE
import dev.slne.minestom.lobby.api.command.commandapi.executor.SyntaxFailurePolicy
import dev.slne.minestom.lobby.api.command.commandapi.executor.handleCommandFailure
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import revxrsal.commands.minestom.actor.MinestomCommandActor

/**
 * Runs suspending command work outside Lamp's synchronous invocation without duplicating
 * coroutine and error handling in every plugin.
 */
fun MinestomCommandActor.launchAsyncCommand(
    commandName: String,
    knownFailure: (Throwable) -> Component? = { null },
    unexpectedFailure: Component = GENERIC_COMMAND_FAILURE,
    block: suspend MinestomCommandActor.() -> Unit,
): Job = minestomScope.launch {
    try {
        block(this@launchAsyncCommand)
    } catch (failure: Throwable) {
        handleCommandFailure(
            sender = sender(),
            input = commandName,
            failure = failure,
            knownFailure = knownFailure,
            unexpectedFailure = unexpectedFailure,
            syntaxFailurePolicy = SyntaxFailurePolicy.USE_UNEXPECTED_FAILURE,
        )
    }
}

fun MinestomCommandActor.launchAsyncPlayerCommand(
    commandName: String,
    knownFailure: (Throwable) -> Component? = { null },
    unexpectedFailure: Component = GENERIC_COMMAND_FAILURE,
    block: suspend (Player) -> Unit,
): Job {
    val player = requirePlayer()
    return launchAsyncCommand(commandName, knownFailure, unexpectedFailure) {
        block(player)
    }
}

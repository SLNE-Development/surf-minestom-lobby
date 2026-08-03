package dev.slne.minestom.lobby.api.coroutine

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import net.minestom.server.MinecraftServer

internal object MinestomScopeProvider {

    val scope: CoroutineScope

    init {
        val exceptionHandler = CoroutineExceptionHandler { context, throwable ->
            MinecraftServer.LOGGER.error("Coroutine exception in context: $context", throwable)
        }
        val rootScope = CoroutineScope(exceptionHandler)

        scope = rootScope + SupervisorJob() + MinestomDispatchers.Main
    }
}

val minestomScope get() = MinestomScopeProvider.scope

fun main() {
}
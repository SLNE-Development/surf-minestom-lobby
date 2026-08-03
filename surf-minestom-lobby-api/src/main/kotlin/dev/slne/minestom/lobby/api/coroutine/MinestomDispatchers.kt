package dev.slne.minestom.lobby.api.coroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.withContext
import net.minestom.server.MinecraftServer
import kotlin.coroutines.CoroutineContext

object MinestomDispatchers {
    val Main = object : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            MinecraftServer.getSchedulerManager().scheduleNextTick(block)
        }
    }
}

suspend fun main() {
    withContext(MinestomDispatchers.Main) {

    }
}
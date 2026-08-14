package dev.slne.minestom.lobby.server.command.commandapi

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.coroutine.minestomAsyncScope
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.network.packet.server.play.TabCompletePacket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

@Singleton
class MinestomSuggestionService @Inject constructor() : AutoCloseable {
    private data class ActiveRequest(
        val transactionId: Int,
        val job: Job,
    )

    private val active = ConcurrentHashMap<UUID, ActiveRequest>()
    private val closed = AtomicBoolean()
    private var scopeProvider: () -> CoroutineScope = { minestomAsyncScope }
    private val serviceScope = lazy {
        val parent = scopeProvider()
        CoroutineScope(
            parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]),
        )
    }

    internal constructor(scopeProvider: () -> CoroutineScope) : this() {
        this.scopeProvider = scopeProvider
    }

    @Synchronized
    internal fun submit(
        player: Player,
        transactionId: Int,
        request: MinestomSuggestionRequest,
    ) {
        if (closed.get()) return
        val playerUuid = player.uuid
        val job = serviceScope.value.launch(start = CoroutineStart.LAZY) {
            val entries = try {
                request.resolve()
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Throwable) {
                MinecraftServer.LOGGER.error(
                    "Failed suggestions for command={}, argument={}, provider={}, input={}, player={}",
                    request.commandName,
                    request.argumentName,
                    request.providerDescription,
                    request.input,
                    playerUuid,
                    failure,
                )
                emptyList()
            }

            val self = coroutineContext.job
            synchronized(this@MinestomSuggestionService) {
                val latest = active[playerUuid]
                if (
                    closed.get() ||
                    !player.isOnline ||
                    latest?.transactionId != transactionId ||
                    latest.job !== self
                ) {
                    return@synchronized
                }

                val matches = ObjectArrayList<TabCompletePacket.Match>(entries.size)
                entries.forEach { entry ->
                    matches += TabCompletePacket.Match(entry.suggestion, entry.tooltip)
                }

                player.sendPacket(
                    TabCompletePacket(
                        transactionId,
                        request.range.start,
                        request.range.length,
                        matches,
                    ),
                )
            }
        }
        val next = ActiveRequest(transactionId, job)
        active.put(playerUuid, next)?.job?.cancel(
            CancellationException("Superseded completion request"),
        )
        job.invokeOnCompletion { active.remove(playerUuid, next) }
        job.start()
    }

    @Synchronized
    internal fun cancel(player: Player) {
        active.remove(player.uuid)?.job?.cancel(
            CancellationException("Player disconnected"),
        )
    }

    @Synchronized
    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        if (serviceScope.isInitialized()) {
            serviceScope.value.cancel(CancellationException("CommandAPI stopped"))
        }
        active.clear()
    }
}

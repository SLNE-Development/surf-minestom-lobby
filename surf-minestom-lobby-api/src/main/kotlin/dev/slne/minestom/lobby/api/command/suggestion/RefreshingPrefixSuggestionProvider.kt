package dev.slne.minestom.lobby.api.command.suggestion

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.minestom.actor.MinestomCommandActor
import revxrsal.commands.node.ExecutionContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A non-blocking suggestion provider for candidates that require suspending I/O to refresh.
 *
 * Command completion always reads the last immutable snapshot. Missing or stale snapshots are
 * refreshed in [scope], never on Minestom's command thread. Call [prime] during plugin startup or
 * player lifecycle events when suggestions must be available on the first completion request.
 */
class RefreshingPrefixSuggestionProvider<K : Any>(
    private val scope: CoroutineScope,
    private val key: (ExecutionContext<MinestomCommandActor>) -> K?,
    private val loader: suspend (K) -> Collection<String>,
    private val limit: Int = 100,
    private val refreshAfter: Duration = 30.seconds,
    private val onRefreshFailure: (K, Throwable) -> Unit = { _, _ -> },
) : SuggestionProvider<MinestomCommandActor> {
    private class Snapshot {
        @Volatile
        var candidates: List<String> = emptyList()

        @Volatile
        var refreshedAtMillis: Long = Long.MIN_VALUE

        val refreshing = AtomicBoolean()
    }

    private val snapshots = ConcurrentHashMap<K, Snapshot>()

    init {
        require(limit >= 0) { "limit must not be negative" }
        require(!refreshAfter.isNegative()) { "refreshAfter must not be negative" }
    }

    override fun getSuggestions(
        context: ExecutionContext<MinestomCommandActor>,
    ): Collection<String> {
        val cacheKey = key(context) ?: return emptyList()
        val snapshot = snapshots.computeIfAbsent(cacheKey) { Snapshot() }
        refreshIfStale(cacheKey, snapshot)
        return prefixFiltered(snapshot.candidates, context.currentArgument(), limit)
    }

    /** Loads and stores [cacheKey] before command completion needs it. */
    suspend fun prime(cacheKey: K) {
        val snapshot = snapshots.computeIfAbsent(cacheKey) { Snapshot() }
        refreshNow(cacheKey, snapshot)
    }

    fun invalidate(cacheKey: K) {
        snapshots.remove(cacheKey)
    }

    fun invalidateAll() {
        snapshots.clear()
    }

    private fun refreshIfStale(cacheKey: K, snapshot: Snapshot) {
        if (snapshot.refreshedAtMillis != Long.MIN_VALUE) {
            val age = System.currentTimeMillis() - snapshot.refreshedAtMillis
            if (age < refreshAfter.inWholeMilliseconds) return
        }
        if (!snapshot.refreshing.compareAndSet(false, true)) return

        scope.launch {
            try {
                loadSnapshot(cacheKey, snapshot)
            } finally {
                snapshot.refreshing.set(false)
            }
        }
    }

    private suspend fun refreshNow(cacheKey: K, snapshot: Snapshot) {
        if (!snapshot.refreshing.compareAndSet(false, true)) return
        try {
            loadSnapshot(cacheKey, snapshot)
        } finally {
            snapshot.refreshing.set(false)
        }
    }

    private suspend fun loadSnapshot(cacheKey: K, snapshot: Snapshot) {
        try {
            snapshot.candidates = loader(cacheKey)
                .distinctBy { it.lowercase() }
            snapshot.refreshedAtMillis = System.currentTimeMillis()
        } catch (failure: Throwable) {
            onRefreshFailure(cacheKey, failure)
        }
    }
}

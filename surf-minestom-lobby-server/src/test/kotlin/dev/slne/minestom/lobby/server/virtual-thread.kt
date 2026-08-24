package dev.slne.minestom.lobby.server

import java.util.concurrent.CompletableFuture

/**
 * Runs [block] on a virtual thread and returns what it produced.
 *
 * Logging a player in and dispatching an `AsyncEvent` both assert that they run on one.
 */
internal fun <T> onVirtualThread(block: () -> T): T {
    val result = CompletableFuture<T>()

    Thread.startVirtualThread {
        runCatching(block)
            .onSuccess(result::complete)
            .onFailure(result::completeExceptionally)
    }

    return result.join()
}

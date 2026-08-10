package dev.slne.minestom.lobby.server.bootstrap

import dev.slne.minestom.lobby.server.config.ServerConfig

private const val DISPATCHER_THREADS_PROPERTY = "minestom.dispatcher-threads"

internal fun ServerConfig.PerformanceConfig.applyTickDispatcherThreads() {
    val existing = System.getProperty(DISPATCHER_THREADS_PROPERTY)
    if (existing != null) {
        bootstrapLogger.info(
            "Tick dispatcher threads pinned via -D{}={}; keeping it.",
            DISPATCHER_THREADS_PROPERTY,
            existing
        )
        return
    }

    val threads = if (tickThreads <= 0) Runtime.getRuntime().availableProcessors() else tickThreads

    System.setProperty(DISPATCHER_THREADS_PROPERTY, threads.toString())
    bootstrapLogger.info("Using {} tick dispatcher thread(s).", threads)
}

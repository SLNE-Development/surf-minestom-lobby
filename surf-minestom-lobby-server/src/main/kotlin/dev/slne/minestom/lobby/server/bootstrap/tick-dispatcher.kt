package dev.slne.minestom.lobby.server.bootstrap

import net.minestom.server.property.ServerProperties
import org.spongepowered.configurate.ConfigurationNode

private const val DISPATCHER_THREADS_PROPERTY = "minestom.dispatcher-threads"

/**
 * Default of `performance.tick-threads`; must match `ServerConfig.PerformanceConfig.tickThreads`.
 */
private const val DEFAULT_TICK_THREADS = 1

/**
 * Sets the number of tick dispatcher threads from `performance.tick-threads` of the raw
 * configuration [root].
 *
 * Minestom reads its properties once, when [ServerProperties] is initialized, so this has to run
 * before anything touches Minestom, including the deserialization of `ServerConfig`.
 *
 * @return the number of threads set, or `null` when it was pinned with `-D`
 */
internal fun applyTickDispatcherThreads(root: ConfigurationNode): Int? {
    val existing = System.getProperty(DISPATCHER_THREADS_PROPERTY)
    if (existing != null) {
        bootstrapLogger.info(
            "Tick dispatcher threads pinned via -D{}={}; keeping it.",
            DISPATCHER_THREADS_PROPERTY,
            existing
        )
        return null
    }

    val tickThreads = root.node("performance", "tick-threads").getInt(DEFAULT_TICK_THREADS)
    val threads = if (tickThreads <= 0) Runtime.getRuntime().availableProcessors() else tickThreads

    System.setProperty(DISPATCHER_THREADS_PROPERTY, threads.toString())
    bootstrapLogger.info("Using {} tick dispatcher thread(s).", threads)
    return threads
}

/**
 * Warns when Minestom read its properties before [applyTickDispatcherThreads] ran.
 */
internal fun warnIfTickDispatcherThreadsIgnored(threads: Int) {
    val actual = ServerProperties.DISPATCHER_THREADS.get()
    if (actual != threads) {
        bootstrapLogger.warn(
            "Minestom was initialized before the tick dispatcher threads were set; using {} instead of {}.",
            actual,
            threads,
        )
    }
}

package dev.slne.minestom.lobby.server.bootstrap

private const val KEEP_ALIVE_DELAY_PROPERTY = "minestom.keep-alive-delay"
private const val KEEP_ALIVE_DELAY_MILLIS = 2_000L

/**
 * Applies the keep alive delay.
 *
 * Velocity-CTD drops a backend that stays silent for
 * `login-timeout` (6s by default) until the player reaches the play phase, but a player can be held
 * in the configuration phase indefinitely. Paper sends
 * keep alives every second, so we send them every 2 seconds to avoid triggering the timeout.
 */
internal fun applyKeepAliveDelay() {
    val existing = System.getProperty(KEEP_ALIVE_DELAY_PROPERTY)
    if (existing != null) {
        bootstrapLogger.info(
            "Keep alive delay pinned via -D{}={}; keeping it.",
            KEEP_ALIVE_DELAY_PROPERTY,
            existing
        )
        return
    }

    System.setProperty(KEEP_ALIVE_DELAY_PROPERTY, KEEP_ALIVE_DELAY_MILLIS.toString())
    bootstrapLogger.info("Sending keep alives every {}ms.", KEEP_ALIVE_DELAY_MILLIS)
}

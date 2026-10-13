package dev.slne.minestom.lobby.server.bootstrap

import org.apache.logging.log4j.LogManager
import kotlin.system.exitProcess

/**
 * Exits the process with [status] once everything logged so far has been written.
 *
 * log4j2.xml logs through an async appender and disables Log4j's own shutdown hook, so exiting
 * right after logging would drop the last messages - typically the reason for the exit.
 */
fun exitFlushingLogs(status: Int): Nothing {
    LogManager.shutdown()
    exitProcess(status)
}

package dev.slne.minestom.lobby.server.bootstrap

import net.kyori.adventure.text.logger.slf4j.ComponentLogger

/**
 * Logger for everything that happens before Minestom's own logger exists.
 */
internal val bootstrapLogger: ComponentLogger = ComponentLogger.logger("Bootstrap")

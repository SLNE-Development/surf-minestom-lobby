package dev.slne.minestom.lobby.server.command.commandapi

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.translation.TranslationStore
import net.minestom.server.adventure.MinestomAdventure
import net.minestom.server.command.CommandSender
import net.minestom.server.entity.Player
import java.text.MessageFormat
import java.util.Locale

/**
 * Server-side fallbacks for the vanilla command translation keys.
 *
 * Players resolve these keys in their own client language. Senders without a client have no
 * translator of their own, so the same keys are registered here for [sendTranslated].
 */
object CommandAPITranslations {
    private val STORE_KEY = Key.key("surf", "commandapi")

    @Volatile
    private var registered = false

    fun register() {
        if (registered) return
        registered = true

        val store = TranslationStore.messageFormat(STORE_KEY)
        store.defaultLocale(Locale.US)

        store.registerAll(
            Locale.US,
            mapOf(
                "command.unknown.command" to
                        MessageFormat("Unknown or incomplete command. See below for error"),
                "command.unknown.argument" to MessageFormat("Incorrect argument for command"),
                "command.context.here" to MessageFormat("<--[HERE]"),
            ),
        )

        GlobalTranslator.translator().addSource(store)
    }
}


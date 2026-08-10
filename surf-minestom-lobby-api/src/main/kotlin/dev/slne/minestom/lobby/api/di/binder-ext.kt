package dev.slne.minestom.lobby.api.di

import com.google.inject.Binder
import com.google.inject.multibindings.Multibinder
import dev.slne.minestom.lobby.api.command.CommandRegistrar
import dev.slne.minestom.lobby.api.event.EventRegistrar

/**
 * The set binder for [T], created on first use and shared by every module that asks for it.
 */
inline fun <reified T : Any> Binder.setBinder(): Multibinder<T> =
    Multibinder.newSetBinder(this, T::class.java)

/**
 * Adds [I] to the set of [T]s.
 *
 * ```
 * binder().bindIntoSet<ContextProvider, ServerContextProvider>()
 * ```
 */
inline fun <reified T : Any, reified I : T> Binder.bindIntoSet() {
    setBinder<T>().addBinding().to(I::class.java)
}

/**
 * Registers [T] as an [EventRegistrar], so the server hands it its event node during startup.
 */
inline fun <reified T : EventRegistrar> Binder.bindEventRegistrar() =
    bindIntoSet<EventRegistrar, T>()

/**
 * Registers [T] as a [CommandRegistrar], so the server hands it Lamp during startup.
 */
inline fun <reified T : CommandRegistrar> Binder.bindCommandRegistrar() =
    bindIntoSet<CommandRegistrar, T>()

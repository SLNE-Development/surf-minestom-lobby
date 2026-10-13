package dev.slne.minestom.lobby.server.di

import com.google.inject.Binder
import com.google.inject.multibindings.Multibinder
import dev.slne.surf.api.minestom.event.EventRegistrar

/**
 * The set binder for [T], created on first use and shared by every module that asks for it.
 */
inline fun <reified T : Any> Binder.setBinder(): Multibinder<T> =
    Multibinder.newSetBinder(this, T::class.java)

/**
 * Adds [I] to the set of [T]s.
 */
inline fun <reified T : Any, reified I : T> Binder.bindIntoSet() {
    setBinder<T>().addBinding().to(I::class.java)
}

/**
 * Registers [T] as an [EventRegistrar], so its listeners are added to the surf api's event node.
 */
inline fun <reified T : EventRegistrar> Binder.bindEventRegistrar() =
    bindIntoSet<EventRegistrar, T>()

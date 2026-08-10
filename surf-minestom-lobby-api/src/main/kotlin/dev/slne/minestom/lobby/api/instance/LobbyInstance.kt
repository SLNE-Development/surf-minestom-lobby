package dev.slne.minestom.lobby.api.instance

import com.google.inject.BindingAnnotation

/**
 * The main lobby world. This is the world that players will be sent to when they join the server.
 *
 * The world is loaded while the server starts up, so this binding is only resolvable afterwards -
 * during a plugin's `start`, in an event listener, or through a `Provider`. Injecting it into
 * something the injector builds eagerly (an `asEagerSingleton` binding, for instance) fails, because
 * at that point there is no world yet.
 */
@BindingAnnotation
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
)
annotation class LobbyInstance
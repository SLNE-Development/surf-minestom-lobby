package dev.slne.minestom.lobby.api.instance

import com.google.inject.BindingAnnotation

/**
 * The main lobby world. This is the world that players will be sent to when they join the server.
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
package dev.slne.minestom.lobby.server.config.contraints

import org.spongepowered.configurate.objectmapping.meta.Constraint
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class NonBlank {

    object Factory : Constraint.Factory<NonBlank, String> {
        override fun make(data: NonBlank, type: Type): Constraint<String> = Constraint { value ->
            if (value.isNullOrBlank()) {
                throw SerializationException("Value cannot be blank")
            }
        }
    }
}

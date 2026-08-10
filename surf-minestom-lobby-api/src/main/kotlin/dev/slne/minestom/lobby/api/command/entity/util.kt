package dev.slne.minestom.lobby.api.command.entity

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Entity
import net.minestom.server.entity.metadata.EntityMeta

val Entity.displayName: Component
    get() = get(DataComponents.CUSTOM_NAME) ?: text(this.entityType.key().asMinimalString())

inline fun <reified M : EntityMeta> Entity.editEntityMeta(noinline editor: (M) -> Unit) {
    editEntityMeta(M::class.java, editor)
}
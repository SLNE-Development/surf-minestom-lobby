package dev.slne.minestom.lobby.api.command.entity

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Entity

val Entity.displayName: Component
    get() = get(DataComponents.CUSTOM_NAME) ?: text(this.entityType.key().asMinimalString())
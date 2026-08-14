/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.minestom.lobby.api.command.commandapi.argument

import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player

class PlayerArgument(nodeName: String) : Argument<Player>(nodeName) {
    override val kind = ArgumentKind.Player

    override fun stringify(value: Player): String = value.username
}

class PlayersArgument(
    nodeName: String,
    val allowEmpty: Boolean = false,
) : Argument<List<Player>>(nodeName) {
    override val kind = ArgumentKind.Players(allowEmpty)

    override fun stringify(value: List<Player>): String =
        value.joinToString(",", transform = Player::getUsername)
}

class EntityArgument(nodeName: String) : Argument<Entity>(nodeName) {
    override val kind = ArgumentKind.Entity

    override fun stringify(value: Entity): String = value.uuid.toString()
}

class EntitiesArgument(
    nodeName: String,
    val allowEmpty: Boolean = false,
) : Argument<List<Entity>>(nodeName) {
    override val kind = ArgumentKind.Entities(allowEmpty)

    override fun stringify(value: List<Entity>): String =
        value.joinToString(",") { entity -> entity.uuid.toString() }
}

class EntityTypeArgument(nodeName: String) : Argument<EntityType>(nodeName) {
    override val kind = ArgumentKind.EntityType

    override fun stringify(value: EntityType): String = value.key().asString()
}

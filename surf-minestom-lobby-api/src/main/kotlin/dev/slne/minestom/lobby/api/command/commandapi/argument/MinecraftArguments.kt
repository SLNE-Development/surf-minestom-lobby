/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.minestom.lobby.api.command.commandapi.argument

import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.BinaryTag
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minestom.server.MinecraftServer
import net.minestom.server.adventure.MinestomAdventure
import net.minestom.server.color.TeamColor
import net.minestom.server.entity.GameMode
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.particle.Particle
import java.time.Duration

class BlockStateArgument(nodeName: String) : Argument<Block>(nodeName) {
    override val kind = ArgumentKind.BlockState

    override fun stringify(value: Block): String = value.state()
}

class ItemStackArgument(nodeName: String) : Argument<ItemStack>(nodeName) {
    override val kind = ArgumentKind.ItemStack

    override fun stringify(value: ItemStack): String = value.material().name()
}

class ComponentArgument(nodeName: String) : Argument<Component>(nodeName) {
    override val kind = ArgumentKind.Component

    override fun stringify(value: Component): String = GsonComponentSerializer.gson().serialize(value)
}

class NBTArgument(nodeName: String) : Argument<BinaryTag>(nodeName) {
    override val kind = ArgumentKind.Nbt

    override fun stringify(value: BinaryTag): String = MinestomAdventure.tagStringIO().asString(value)
}

class NBTCompoundArgument(nodeName: String) : Argument<CompoundBinaryTag>(nodeName) {
    override val kind = ArgumentKind.NbtCompound

    override fun stringify(value: CompoundBinaryTag): String = MinestomAdventure.tagStringIO().asString(value)
}

class ResourceLocationArgument(nodeName: String) : Argument<Key>(nodeName) {
    override val kind = ArgumentKind.ResourceLocation

    override fun stringify(value: Key): String = value.asString()
}

/**
 * A duration parsed from vanilla's suffixed time syntax (`50d`, `25s`, `75t`, or a bare tick
 * count).
 */
class TimeArgument(nodeName: String) : Argument<Duration>(nodeName) {
    override val kind = ArgumentKind.Time

    override fun stringify(value: Duration): String = "${value.toMillis() / MinecraftServer.TICK_MS}t"
}

class TeamColorArgument(nodeName: String) : Argument<TeamColor>(nodeName) {
    override val kind = ArgumentKind.TeamColor

    override fun stringify(value: TeamColor): String = value.toString()
}

class ParticleArgument(nodeName: String) : Argument<Particle>(nodeName) {
    override val kind = ArgumentKind.Particle

    override fun stringify(value: Particle): String = value.name()
}

/**
 * A player game mode parsed case-insensitively from its vanilla name (`survival`, `creative`,
 * `adventure`, or `spectator`).
 */
class GameModeArgument(nodeName: String) : Argument<GameMode>(nodeName) {
    override val kind = ArgumentKind.GameMode

    override fun stringify(value: GameMode): String = value.name.lowercase()
}

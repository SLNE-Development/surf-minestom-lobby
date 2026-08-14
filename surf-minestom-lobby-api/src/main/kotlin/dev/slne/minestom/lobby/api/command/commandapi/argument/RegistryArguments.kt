/*
 * Substantially translated from CommandAPI 12.0.0 (https://github.com/CommandAPI/CommandAPI).
 * MIT License, Copyright (c) 2020 - 2022 Jorel Ali.
 * The complete license is distributed in META-INF/LICENSES/CommandAPI-LICENSE.txt.
 */
package dev.slne.minestom.lobby.api.command.commandapi.argument

import net.minestom.server.instance.Instance
import net.minestom.server.item.enchant.Enchantment
import net.minestom.server.potion.PotionEffect
import net.minestom.server.registry.Registry
import net.minestom.server.registry.RegistryKey
import net.minestom.server.sound.SoundEvent
import net.minestom.server.world.biome.Biome

class SoundArgument(nodeName: String) : Argument<SoundEvent>(nodeName) {
    override val kind = ArgumentKind.Sound
    override fun stringify(value: SoundEvent): String = value.name()
}

class PotionEffectArgument(nodeName: String) : Argument<PotionEffect>(nodeName) {
    override val kind = ArgumentKind.PotionEffect
    override fun stringify(value: PotionEffect): String = value.key().asString()
}

class BiomeArgument(nodeName: String) : Argument<RegistryKey<Biome>>(nodeName) {
    override val kind = ArgumentKind.Biome
    override fun stringify(value: RegistryKey<Biome>): String = value.name()
}

class EnchantmentArgument(nodeName: String) : Argument<RegistryKey<Enchantment>>(nodeName) {
    override val kind = ArgumentKind.Enchantment
    override fun stringify(value: RegistryKey<Enchantment>): String = value.name()
}

/**
 * A resource resolved from an arbitrary [Registry] by namespaced key, advertised to the client
 * under [identifier].
 */
class ResourceArgument<T>(
    nodeName: String,
    identifier: String,
    registry: Registry<T>,
) : Argument<RegistryKey<T>>(nodeName) {
    override val kind = ArgumentKind.Resource(identifier, registry)
    override fun stringify(value: RegistryKey<T>): String = value.name()
}

/**
 * A live [Instance], resolved by its UUID or by its dimension name when that name is unique
 * among currently registered instances.
 */
class InstanceArgument(nodeName: String) : Argument<Instance>(nodeName) {
    override val kind = ArgumentKind.Instance
    override fun stringify(value: Instance): String = value.uuid.toString()
}

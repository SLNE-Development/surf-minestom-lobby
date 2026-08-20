package dev.slne.minestom.lobby.api.placeholder

import io.github.miniplaceholders.api.MiniPlaceholders
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

fun miniPlaceholders(): TagResolver = TagResolver.resolver(
    MiniPlaceholders.globalPlaceholders(),
    MiniPlaceholders.audiencePlaceholders(),
    MiniPlaceholders.relationalPlaceholders(),
    MiniPlaceholders.relationalGlobalPlaceholders(),
    MiniPlaceholders.audienceGlobalPlaceholders()
)

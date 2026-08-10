package dev.slne.minestom.lobby.server.world.generator

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.generator.GenerationUnit
import net.minestom.server.instance.generator.Generator

object FlatWorldGenerator : Generator {

    const val BEDROCK_HEIGHT = 0
    const val SURFACE_HEIGHT = 64

    override fun generate(unit: GenerationUnit) {
        with(unit.modifier()) {
            fillHeight(BEDROCK_HEIGHT, BEDROCK_HEIGHT + 1, Block.BEDROCK)
            fillHeight(BEDROCK_HEIGHT + 1, SURFACE_HEIGHT, Block.DIRT)
            fillHeight(SURFACE_HEIGHT, SURFACE_HEIGHT + 1, Block.GRASS_BLOCK)
        }
    }
}

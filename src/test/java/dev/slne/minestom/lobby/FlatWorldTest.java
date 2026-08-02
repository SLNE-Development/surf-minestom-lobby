package dev.slne.minestom.lobby;

import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnvTest
class FlatWorldTest {
    @Test
    void generatesVanillaFlatWorld(Env env) {
        InstanceContainer instance = Main.createInstance();
        try {
            instance.loadChunk(0, 0).join();

            assertEquals(Block.BEDROCK, instance.getBlock(0, 0, 0));
            assertEquals(Block.DIRT, instance.getBlock(0, 1, 0));
            assertEquals(Block.DIRT, instance.getBlock(0, 2, 0));
            assertEquals(Block.GRASS_BLOCK, instance.getBlock(0, 3, 0));
            assertEquals(Block.AIR, instance.getBlock(0, 4, 0));
        } finally {
            env.destroyInstance(instance);
        }
    }
}

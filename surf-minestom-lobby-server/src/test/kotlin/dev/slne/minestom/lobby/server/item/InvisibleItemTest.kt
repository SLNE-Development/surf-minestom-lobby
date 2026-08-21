package dev.slne.minestom.lobby.server.item

import dev.slne.minestom.lobby.api.item.INVISIBLE_ITEM_MODEL
import dev.slne.minestom.lobby.api.item.invisibleItem
import net.minestom.server.component.DataComponents
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

@EnvTest
class InvisibleItemTest {

    @Test
    fun `invisible item carries the transparent item model`(env: Env) {
        val item = invisibleItem()

        assertFalse(item.isAir)
        assertEquals(INVISIBLE_ITEM_MODEL, item.get(DataComponents.ITEM_MODEL))
    }
}

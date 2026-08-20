package dev.slne.minestom.lobby.server.world

import dev.slne.minestom.lobby.api.instance.setWorldKey
import dev.slne.minestom.lobby.api.instance.worldKey
import dev.slne.minestom.lobby.api.key.SurfKey
import net.kyori.adventure.key.InvalidKeyException
import net.kyori.adventure.key.Key
import net.minestom.server.MinecraftServer
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EnvTest
class WorldKeyTest {

    @Test
    fun `an instance without a named world reports no key`(env: Env) {
        val instance = MinecraftServer.getInstanceManager().createInstanceContainer()

        try {
            assertNull(instance.worldKey)
        } finally {
            env.destroyInstance(instance)
        }
    }

    @Test
    fun `an instance reports the key its world was loaded under`(env: Env) {
        val instance = MinecraftServer.getInstanceManager().createInstanceContainer()

        try {
            instance.setWorldKey(SurfKey.key("lobby-2024"))

            assertEquals(SurfKey.key("lobby-2024"), instance.worldKey)
        } finally {
            env.destroyInstance(instance)
        }
    }

    @Test
    fun `a stored world name is addressed under the surf namespace`() {
        assertEquals(SurfKey.NAMESPACE, SurfKey.key("lobby").namespace())
        assertEquals("lobby", SurfKey.key("lobby").value())
    }

    @Test
    fun `a world name that cannot be part of a key is rejected`() {
        assertThrows(InvalidKeyException::class.java) { SurfKey.key("Lobby 2024") }
    }

    @Test
    fun `a surf key equals a plain key of the same name in both directions`() {
        val surfKey: Any = SurfKey.key("lobby")
        val plainKey: Any = Key.key(SurfKey.NAMESPACE, "lobby")

        assertEquals(plainKey.hashCode(), surfKey.hashCode())
        assertTrue(plainKey == surfKey)
        assertTrue(surfKey == plainKey)
    }

    @Test
    fun `a surf key finds itself in a map it was stored in as a plain key`() {
        val pads = mapOf<Key, String>(Key.key(SurfKey.NAMESPACE, "lobby") to "pad")

        assertEquals("pad", pads[SurfKey.key("lobby")])
    }

    @Test
    fun `a surf key does not equal a key of another name`() {
        val surfKey: Any = SurfKey.key("lobby")

        assertFalse(surfKey == Key.key(SurfKey.NAMESPACE, "arena"))
        assertFalse(surfKey == Key.key("minecraft", "lobby"))
    }

    @Test
    fun `a surf key reads as the key it stands for`() {
        assertEquals("surf:lobby", SurfKey.key("lobby").toString())
    }

    @Test
    fun `replacing the world hands out a new key while the dimension stays the same`(env: Env) {
        val instanceManager = MinecraftServer.getInstanceManager()
        val current = instanceManager.createInstanceContainer()
        val replacement = instanceManager.createInstanceContainer()

        try {
            current.setWorldKey(SurfKey.key("lobby-2024"))
            replacement.setWorldKey(SurfKey.key("lobby-2025"))

            assertEquals(current.dimensionName, replacement.dimensionName)
            assertNotEquals(current.worldKey, replacement.worldKey)
        } finally {
            env.destroyInstance(current)
            env.destroyInstance(replacement)
        }
    }
}

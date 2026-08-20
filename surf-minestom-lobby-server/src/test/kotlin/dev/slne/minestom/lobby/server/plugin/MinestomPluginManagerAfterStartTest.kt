package dev.slne.minestom.lobby.server.plugin

import com.google.inject.Guice
import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.Stage
import dev.slne.minestom.lobby.api.plugin.MinestomPlugin
import dev.slne.minestom.lobby.api.plugin.MinestomPluginEntrypoint
import dev.slne.minestom.lobby.api.plugin.annotation.MinestomPluginMeta
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * `afterStart` exists so that a plugin can act on what every other plugin brought with it, so the
 * whole point is that no plugin sees it before all of them have started.
 */
class MinestomPluginManagerAfterStartTest {

    @BeforeEach
    fun resetLog() {
        log.clear()
    }

    @Test
    fun `every plugin starts before the first one is told that everyone started`() = runTest {
        manager(FirstPlugin(), SecondPlugin()).startAll()

        assertEquals(
            listOf("first:start", "second:start", "first:afterStart", "second:afterStart"),
            log.toList()
        )
    }

    @Test
    fun `a plugin is told that everyone started after the plugins it depends on`() = runTest {
        manager(SecondPlugin(), FirstPlugin()).startAll()

        assertEquals(
            listOf("first:start", "second:start", "first:afterStart", "second:afterStart"),
            log.toList()
        )
    }

    @Test
    fun `a plugin failing once everyone started brings every plugin back down`() = runTest {
        val manager = manager(FirstPlugin(), FailingAfterStartPlugin())

        val failure = runCatching { manager.startAll() }.exceptionOrNull()

        assertInstanceOf(IllegalStateException::class.java, failure)

        assertEquals(
            listOf("first:start", "failing:start", "first:afterStart", "failing:afterStart"),
            log.toList().filterNot { it.endsWith(":stop") }
        )
        assertEquals(listOf("failing:stop", "first:stop"), log.toList().filter { it.endsWith(":stop") })
    }

    @Test
    fun `starting twice is refused`() = runTest {
        val manager = manager(FirstPlugin())
        manager.startAll()

        val failure = runCatching { manager.startAll() }.exceptionOrNull()

        assertInstanceOf(IllegalStateException::class.java, failure)
    }

    private fun manager(vararg plugins: MinestomPlugin): MinestomPluginManager {
        val modules = plugins.map { PluginModule(it, DATA_DIRECTORY) }
        val injector = Guice.createInjector(Stage.PRODUCTION, modules)

        return MinestomPluginManager(injector, PluginCatalog(plugins.toList()))
    }

    @MinestomPluginMeta("first-plugin")
    internal class FirstPlugin : MinestomPlugin(FirstEntrypoint::class.java)

    @Singleton
    internal class FirstEntrypoint @Inject constructor() : MinestomPluginEntrypoint {
        override suspend fun start() {
            log += "first:start"
        }
        override suspend fun afterStart() {
            log += "first:afterStart"
        }
        override suspend fun stop() {
            log += "first:stop"
        }
    }

    @MinestomPluginMeta("second-plugin", dependsOn = ["first-plugin"])
    internal class SecondPlugin : MinestomPlugin(SecondEntrypoint::class.java)

    @Singleton
    internal class SecondEntrypoint @Inject constructor() : MinestomPluginEntrypoint {
        override suspend fun start() {
            log += "second:start"
        }
        override suspend fun afterStart() {
            log += "second:afterStart"
        }
        override suspend fun stop() {
            log += "second:stop"
        }
    }

    @MinestomPluginMeta("failing-plugin", dependsOn = ["first-plugin"])
    internal class FailingAfterStartPlugin : MinestomPlugin(FailingAfterStartEntrypoint::class.java)

    @Singleton
    internal class FailingAfterStartEntrypoint @Inject constructor() : MinestomPluginEntrypoint {
        override suspend fun start() {
            log += "failing:start"
        }

        override suspend fun afterStart() {
            log += "failing:afterStart"
            error("cannot finish starting")
        }

        override suspend fun stop() {
            log += "failing:stop"
        }
    }

    private companion object {
        val log = mutableListOf<String>()
        val DATA_DIRECTORY: Path = Path.of("build", "tmp", "after-start-test")
    }
}

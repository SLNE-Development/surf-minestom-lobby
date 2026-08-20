package dev.slne.minestom.lobby.server.integration.miniplaceholders

import dev.slne.minestom.lobby.api.placeholder.miniPlaceholders
import io.github.miniplaceholders.api.Expansion
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.pointer.Pointers
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MiniPlaceholdersResolverTest {

    private val expansion: Expansion = Expansion.builder("lobbytest")
        .globalPlaceholder("server") { _, _ ->
            Tag.selfClosingInserting(Component.text("lobby"))
        }
        .audiencePlaceholder("greeting") { audience, _, _ ->
            Tag.selfClosingInserting(Component.text("hello ${(audience as NamedAudience).name}"))
        }
        .build()

    @AfterEach
    fun unregisterExpansion() {
        if (expansion.registered()) {
            expansion.unregister()
        }
    }

    @Test
    fun `a global placeholder resolves without a deserialization target`() {
        expansion.register()

        assertEquals("on lobby", plain(deserialize("on <lobbytest_server>")))
    }

    @Test
    fun `an audience placeholder resolves against the deserialization target`() {
        expansion.register()

        assertEquals("hello red", plain(deserialize("<lobbytest_greeting>", NamedAudience("red"))))
    }

    @Test
    fun `an audience placeholder stays untouched without a deserialization target`() {
        expansion.register()

        assertEquals("<lobbytest_greeting>", plain(deserialize("<lobbytest_greeting>")))
    }

    @Test
    fun `a placeholder of an expansion that is not registered stays untouched`() {
        assertEquals("<lobbytest_server>", plain(deserialize("<lobbytest_server>")))
    }

    private fun deserialize(input: String, target: Audience? = null): Component =
        if (target == null) {
            MiniMessage.miniMessage().deserialize(input, miniPlaceholders())
        } else {
            MiniMessage.miniMessage().deserialize(input, target, miniPlaceholders())
        }

    private fun plain(component: Component) =
        PlainTextComponentSerializer.plainText().serialize(component)

    private class NamedAudience(val name: String) : Audience {
        override fun pointers(): Pointers = Pointers.empty()
    }
}

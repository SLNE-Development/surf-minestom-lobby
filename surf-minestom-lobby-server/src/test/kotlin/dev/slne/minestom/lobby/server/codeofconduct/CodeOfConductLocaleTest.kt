package dev.slne.minestom.lobby.server.codeofconduct

import dev.slne.minestom.lobby.server.database.codeofconduct.codeOfConductLocaleKey
import dev.slne.minestom.lobby.server.database.codeofconduct.parseCodeOfConductLocale
import java.util.Locale
import net.minestom.server.network.player.ClientSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CodeOfConductLocaleTest {

    @Test
    fun `a file name resolves to the locale a client sends`() {
        assertEquals(Locale.GERMANY, parseCodeOfConductLocale("de_de"))
        assertEquals(Locale.US, parseCodeOfConductLocale("en_us"))
        assertEquals(Locale.forLanguageTag("pt-BR"), parseCodeOfConductLocale("pt_br"))
        assertEquals(Locale.ENGLISH, parseCodeOfConductLocale("en"))
    }

    @Test
    fun `the default client locale is keyed the way its file is named`() {
        assertEquals("en_us", codeOfConductLocaleKey(ClientSettings.DEFAULT.locale()))
    }

    @Test
    fun `a locale key round trips through its file name`() {
        for (name in listOf("de_de", "en_us", "en", "zh_cn")) {
            assertEquals(name, codeOfConductLocaleKey(parseCodeOfConductLocale(name)))
        }
    }

    @Test
    fun `a name that is no locale is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            parseCodeOfConductLocale("not a locale")
        }
    }
}

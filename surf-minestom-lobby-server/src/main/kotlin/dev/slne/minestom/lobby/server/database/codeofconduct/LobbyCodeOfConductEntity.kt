package dev.slne.minestom.lobby.server.database.codeofconduct

import org.komapper.annotation.KomapperColumn
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.core.type.ClobString
import java.util.Locale

@KomapperEntity
@KomapperTable("lobby_code_of_conduct")
data class LobbyCodeOfConductEntity(
    @KomapperId
    @KomapperColumn(name = "locale_key", length = 32)
    val localeKey: String,

    @KomapperColumn(length = 64)
    val sha256: String,

    @KomapperColumn(name = "conduct_text", alternateType = ClobString::class)
    val text: String,
) {

    fun locale(): Locale = parseCodeOfConductLocale(localeKey)
}


fun parseCodeOfConductLocale(localeKey: String): Locale {
    val locale = Locale.forLanguageTag(localeKey.replace('_', '-'))

    require(locale.language.isNotEmpty()) {
        "'$localeKey' is not a valid locale"
    }

    return locale
}

fun codeOfConductLocaleKey(locale: Locale): String =
    locale.toLanguageTag().replace('-', '_').lowercase(Locale.ROOT)

package dev.slne.minestom.lobby.server.codeofconduct

import com.google.common.hash.Hashing
import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.database.codeofconduct.LobbyCodeOfConductEntity
import dev.slne.minestom.lobby.server.database.codeofconduct.LobbyCodeOfConductRepository
import dev.slne.minestom.lobby.server.database.codeofconduct.codeOfConductLocaleKey
import dev.slne.minestom.lobby.server.database.codeofconduct.parseCodeOfConductLocale
import dev.slne.minestom.lobby.server.lifecycle.LobbyService
import dev.slne.minestom.lobby.server.upload.UploadEntry
import dev.slne.minestom.lobby.server.upload.UploadHandler
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.Locale
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readLines

@Singleton
class CodeOfConductService @Inject constructor(
    private val repository: LobbyCodeOfConductRepository,
    private val config: ServerConfig.CodeOfConductConfig,
) : LobbyService, UploadHandler {

    private companion object {
        val LOGGER = ComponentLogger.logger()
    }

    override val directoryName = "codeofconduct"
    override val fileGlob = "*.txt"

    override val description =
        "Code of conduct texts, one plain UTF-8 text file per locale. Enable them with " +
                "`code-of-conduct.enabled` and pick the fallback with `code-of-conduct.default-locale`."

    override val fileNameDocumentation =
        "`<locale>.txt` - the name without the extension is the locale the text is used for, " +
                "written the way Minecraft does it, for example `en_us.txt` or `de_de.txt`."

    val defaultLocale: Locale = parseCodeOfConductLocale(config.defaultLocale)

    val enabled: Boolean get() = config.enabled

    @Volatile
    var texts: Map<Locale, String> = emptyMap()
        private set

    override suspend fun start() {
        reload()

        if (!config.enabled) {
            return
        }

        check(texts.isNotEmpty()) {
            "The code of conduct is enabled, but no text has been uploaded. Place " +
                    "'<locale>.txt' files into 'upload/$directoryName'."
        }

        check(defaultLocale in texts) {
            "The code of conduct is enabled, but nothing has been uploaded for the default " +
                    "locale '${config.defaultLocale}'. Uploaded locales: ${uploadedLocaleKeys()}"
        }
    }


    fun textFor(locale: Locale?): String? {
        if (!config.enabled) {
            return null
        }

        val texts = texts

        if (locale != null) {
            texts[locale]?.let { return it }

            texts.entries
                .firstOrNull { (uploaded, _) -> uploaded.language == locale.language }
                ?.let { return it.value }
        }

        return texts[defaultLocale]
    }

    override suspend fun publish(file: Path) {
        val locale = parseCodeOfConductLocale(file.nameWithoutExtension.lowercase(Locale.ROOT))
        val localeKey = codeOfConductLocaleKey(locale)

        val text = file.readLines()
            .joinToString("\n")
            .removePrefix("\uFEFF")

        require(text.isNotBlank()) { "Code of conduct file '${file.fileName}' is empty" }

        val sha256 = Hashing.sha256()
            .hashString(text, StandardCharsets.UTF_8)
            .toString()

        if (repository.findSha256(localeKey) == sha256) {
            LOGGER.info("Code of conduct '{}' is already up to date.", localeKey)
            return
        }

        repository.upsert(
            LobbyCodeOfConductEntity(
                localeKey = localeKey,
                sha256 = sha256,
                text = text,
            )
        )

        reload()

        LOGGER.info("Published code of conduct '{}' from '{}'.", localeKey, file)
    }

    override suspend fun list(): List<UploadEntry> = repository.findAll().map { entity ->
        val locale = entity.locale()

        UploadEntry(
            key = entity.localeKey,
            detail = buildString {
                append(locale.getDisplayName(Locale.ENGLISH))
                append(", ")
                append(entity.text.length)
                append(" characters")

                if (locale == defaultLocale) {
                    append(", default")
                }
            },
        )
    }

    override suspend fun delete(key: String): Boolean {
        val localeKey = codeOfConductLocaleKey(parseCodeOfConductLocale(key))

        check(!(config.enabled && localeKey == codeOfConductLocaleKey(defaultLocale))) {
            "'$localeKey' is the default locale of the enabled code of conduct " +
                    "(code-of-conduct.default-locale), so deleting it would leave players " +
                    "without a text"
        }

        val deleted = repository.delete(localeKey)

        if (deleted) {
            reload()
        }

        return deleted
    }

    private suspend fun reload() {
        texts = repository.findAll().associate { it.locale() to it.text }

        LOGGER.info("Loaded {} code of conduct text(s): {}", texts.size, uploadedLocaleKeys())
    }

    private fun uploadedLocaleKeys(): List<String> = texts.keys.map(::codeOfConductLocaleKey)
}

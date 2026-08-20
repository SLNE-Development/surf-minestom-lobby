package dev.slne.minestom.lobby.server.upload

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.lifecycle.LobbyService
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.writeText

/**
 * Imports everything operators dropped into the upload directory into the database.
 *
 * Every [UploadHandler] gets its own sub directory, which is created on startup even when it is
 * empty, and is documented in the readme this service rewrites on every start.
 */
@Singleton
class UploadService @Inject constructor(
    handlers: Set<@JvmSuppressWildcards UploadHandler>,
) : LobbyService {

    private companion object {
        val LOGGER = ComponentLogger.logger()
        val UPLOAD_DIRECTORY = Path("upload")
        const val README_NAME = "README.md"
    }

    private val handlersByDirectory = handlers
        .sortedBy { it.directoryName }
        .associateByTo(LinkedHashMap()) { it.directoryName }

    init {
        require(handlersByDirectory.size == handlers.size) {
            "Two upload handlers share a directory name: " +
                    handlers.map { it.directoryName }.sorted()
        }
    }

    val directoryNames: List<String> get() = handlersByDirectory.keys.toList()

    fun handler(directoryName: String): UploadHandler? = handlersByDirectory[directoryName]

    override suspend fun start() {
        UPLOAD_DIRECTORY.createDirectories()
        UPLOAD_DIRECTORY.resolve(README_NAME).writeText(buildReadme())

        for (handler in handlersByDirectory.values) {
            publishAll(handler)
        }
    }

    private suspend fun publishAll(handler: UploadHandler) {
        val directory = directoryOf(handler)
        directory.createDirectories()

        val files = directory
            .listDirectoryEntries(handler.fileGlob)
            .sortedBy { it.name }

        if (files.isEmpty()) {
            return
        }

        LOGGER.info(
            "Publishing {} upload(s) from '{}'.",
            files.size,
            directory
        )

        for (file in files) {
            requireInsideDirectory(file, directory)

            handler.publish(file)
            file.deleteIfExists()
        }
    }

    private fun directoryOf(handler: UploadHandler): Path =
        UPLOAD_DIRECTORY.resolve(handler.directoryName)

    private fun requireInsideDirectory(file: Path, directory: Path) {
        require(file.toRealPath().parent == directory.toRealPath()) {
            "Refusing to read upload '${file.name}' because it links to a file outside of " +
                    "'$directory'"
        }
    }

    private fun buildReadme(): String = buildString {
        appendLine("# Upload directory")
        appendLine()
        appendLine("Drop your files into the sub directories listed below. Every matching file is")
        appendLine("imported into the database while the server starts and is deleted afterwards,")
        appendLine("so an empty sub directory simply means everything has been imported already.")
        appendLine()
        appendLine("Uploading a file again replaces the entry stored under the same key.")
        appendLine()
        appendLine("This file is regenerated on every server start, so do not edit it.")

        for (handler in handlersByDirectory.values) {
            appendLine()
            appendLine("## ${handler.directoryName}")
            appendLine()
            appendLine(handler.description)
            appendLine()
            appendLine("- Directory: `${directoryOf(handler).invariantSeparatorsPathString}`")
            appendLine("- Accepted files: `${handler.fileGlob}`")
            appendLine("- File name: ${handler.fileNameDocumentation}")
        }

        appendLine()
        appendLine("## Managing what has been uploaded")
        appendLine()
        appendLine("- `/uploads list` lists every upload kind and how many entries it holds.")
        appendLine("- `/uploads list <kind>` lists the entries stored for one kind.")
        appendLine("- `/uploads delete <kind> <key>` permanently deletes a single entry.")
    }
}

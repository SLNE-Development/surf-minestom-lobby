package dev.slne.minestom.lobby.server.upload

import java.nio.file.Path

/**
 * One kind of content operators install by dropping files into the server's upload directory.
 *
 * [UploadService] owns the file system side: it creates [directoryName] inside the upload
 * directory, documents the handler in the readme, hands every matching file to [publish] and
 * deletes the file afterwards.
 */
interface UploadHandler {

    /** Name of this handler's sub directory inside the upload directory. */
    val directoryName: String

    /** Glob matching the files this handler accepts, for example `*.polar`. */
    val fileGlob: String

    /** English description of what this handler stores, rendered into the readme. */
    val description: String

    /** English description of how a file has to be named, rendered into the readme. */
    val fileNameDocumentation: String

    /** Stores [file]'s content under the key derived from its file name. */
    suspend fun publish(file: Path)

    /** Everything this handler currently has stored, ordered by key. */
    suspend fun list(): List<UploadEntry>

    /** Removes the entry stored under [key] and returns whether it existed. */
    suspend fun delete(key: String): Boolean
}

/**
 * A single stored entry of an [UploadHandler].
 *
 * @property key the key the entry is stored under, and the key [UploadHandler.delete] takes
 * @property detail additional information shown when the entry is listed
 */
data class UploadEntry(
    val key: String,
    val detail: String? = null,
)

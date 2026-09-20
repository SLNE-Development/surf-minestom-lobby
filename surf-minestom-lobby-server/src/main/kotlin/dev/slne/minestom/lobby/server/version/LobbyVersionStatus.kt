package dev.slne.minestom.lobby.server.version

/**
 * Result of comparing the running version against the newest published release.
 */
sealed interface LobbyVersionStatus {

    /** The running version is the newest published release. */
    data object UpToDate : LobbyVersionStatus

    /** The jar was built locally rather than by the release workflow. */
    data object DevelopmentBuild : LobbyVersionStatus

    /** A newer release exists. */
    data class UpdateAvailable(
        val latestVersion: String,
    ) : LobbyVersionStatus

    /** The release check itself failed. */
    data class CheckFailed(
        val reason: String,
    ) : LobbyVersionStatus
}
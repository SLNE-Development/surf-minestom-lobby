package dev.slne.minestom.lobby.server.version

/**
 * Result of comparing the running build against the newest published one.
 */
sealed interface LobbyVersionStatus {

    /** No newer build was published. */
    data object UpToDate : LobbyVersionStatus

    /** The jar carries no build number, so it was not published by CI. */
    data object DevelopmentBuild : LobbyVersionStatus

    /**
     * [builds] newer builds exist. [atLeast] marks a count cut off by the fetch page size.
     */
    data class Behind(
        val builds: Int,
        val atLeast: Boolean,
        val latestBuildNumber: Int,
        val latestCommit: String?,
    ) : LobbyVersionStatus

    /** The check itself failed; [reason] is a short, printable cause. */
    data class CheckFailed(val reason: String) : LobbyVersionStatus
}

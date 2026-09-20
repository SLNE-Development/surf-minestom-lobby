package dev.slne.minestom.lobby.server.version

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.time.Duration

private const val REPOSITORY = "SLNE-Development/surf-minestom-lobby"

private val CONNECT_TIMEOUT = Duration.ofSeconds(5)
private val REQUEST_TIMEOUT = Duration.ofSeconds(10)

private val TAG_NAME = Regex(""""tag_name"\s*:\s*"([^"]+)"""")
private val VERSION_PATTERN = Regex("""^v?(\d+)\.(\d+)\.(\d+)$""")

const val LOBBY_DOWNLOAD_URL = "https://github.com/$REPOSITORY/releases/latest"

/**
 * Reads the newest stable GitHub release and compares it with the running version.
 */
class LatestReleaseFetcher {

    private val latestReleaseUri =
        URI.create("https://api.github.com/repos/$REPOSITORY/releases/latest")

    private val httpClient: HttpClient by lazy {
        HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }

    fun statusFor(currentVersion: String): LobbyVersionStatus = when (val response = request()) {
        is FetchResult.Failure -> LobbyVersionStatus.CheckFailed(response.reason)
        is FetchResult.Success -> parseLatestRelease(currentVersion, response.body)
    }

    private fun request(): FetchResult {
        val request = HttpRequest.newBuilder(latestReleaseUri)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "surf-minestom-lobby")
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build()

        val response = try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (_: HttpTimeoutException) {
            return FetchResult.Failure("timeout")
        } catch (failure: IOException) {
            return FetchResult.Failure(failure.message ?: "github.com unreachable")
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            return FetchResult.Failure("interrupted")
        }

        if (response.statusCode() != 200) {
            return FetchResult.Failure("HTTP ${response.statusCode()}")
        }

        return FetchResult.Success(response.body())
    }

    private sealed interface FetchResult {
        data class Success(val body: String) : FetchResult
        data class Failure(val reason: String) : FetchResult
    }
}

internal fun parseLatestRelease(
    currentVersion: String,
    body: String,
): LobbyVersionStatus {
    val latestTag = TAG_NAME.find(body)
        ?.groupValues
        ?.getOrNull(1)
        ?: return LobbyVersionStatus.CheckFailed("latest release does not contain a tag")

    val current = ReleaseVersion.parse(currentVersion)
        ?: return LobbyVersionStatus.CheckFailed("invalid current version: $currentVersion")

    val latest = ReleaseVersion.parse(latestTag)
        ?: return LobbyVersionStatus.CheckFailed("invalid latest release version: $latestTag")

    return if (latest > current) {
        LobbyVersionStatus.UpdateAvailable(
            latestVersion = latest.toString(),
        )
    } else {
        LobbyVersionStatus.UpToDate
    }
}

internal data class ReleaseVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<ReleaseVersion> {

    override fun compareTo(other: ReleaseVersion): Int {
        major.compareTo(other.major)
            .takeIf { it != 0 }
            ?.let { return it }

        minor.compareTo(other.minor)
            .takeIf { it != 0 }
            ?.let { return it }

        return patch.compareTo(other.patch)
    }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        fun parse(value: String): ReleaseVersion? {
            val match = VERSION_PATTERN.matchEntire(value.trim())
                ?: return null

            return ReleaseVersion(
                major = match.groupValues[1].toIntOrNull() ?: return null,
                minor = match.groupValues[2].toIntOrNull() ?: return null,
                patch = match.groupValues[3].toIntOrNull() ?: return null,
            )
        }
    }
}
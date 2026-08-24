package dev.slne.minestom.lobby.server.version

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.time.Duration

private const val REPOSITORY = "SLNE-Development/surf-minestom-lobby"
private const val PUBLISH_WORKFLOW = "publish-server.yml"
private const val PUBLISH_BRANCH = "master"

private const val PAGE_SIZE = 100

private val CONNECT_TIMEOUT = Duration.ofSeconds(5)
private val REQUEST_TIMEOUT = Duration.ofSeconds(10)

private val RUN_NUMBER = Regex("\"run_number\"\\s*:\\s*(\\d+)")
private val HEAD_SHA = Regex("\"head_sha\"\\s*:\\s*\"([0-9a-fA-F]{40})\"")

/** The download page for the rolling public build. */
const val LOBBY_DOWNLOAD_URL = "https://github.com/$REPOSITORY/releases/tag/latest"

/**
 * Reads the build numbers of the successful `publish-server.yml` runs on `master` from the
 * GitHub Actions API. Every such run publishes one jar, so a build number gap is a build gap.
 */
class PublishedBuildFetcher {

    private val runsUri: URI = URI.create(
        "https://api.github.com/repos/$REPOSITORY/actions/workflows/$PUBLISH_WORKFLOW/runs" +
                "?branch=$PUBLISH_BRANCH&status=success&exclude_pull_requests=true" +
                "&per_page=$PAGE_SIZE"
    )

    private val httpClient: HttpClient by lazy {
        HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }

    /**
     * Compares [buildNumber] against the published builds.
     */
    fun statusFor(buildNumber: Int): LobbyVersionStatus = when (val response = request()) {
        is FetchResult.Failure -> LobbyVersionStatus.CheckFailed(response.reason)
        is FetchResult.Success -> parseWorkflowRuns(buildNumber, response.body)
    }

    private fun request(): FetchResult {
        val request = HttpRequest.newBuilder(runsUri)
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

/**
 * Turns a `GET /actions/workflows/{workflow}/runs` body into the status of [buildNumber].
 */
internal fun parseWorkflowRuns(buildNumber: Int, body: String): LobbyVersionStatus {
    val publishedBuilds = RUN_NUMBER.findAll(body)
        .mapNotNull { it.groupValues[1].toIntOrNull() }
        .toList()

    val latestBuildNumber = publishedBuilds.maxOrNull()
        ?: return LobbyVersionStatus.CheckFailed("no published builds")

    val newerBuilds = publishedBuilds.count { it > buildNumber }

    if (newerBuilds == 0) {
        return LobbyVersionStatus.UpToDate
    }

    return LobbyVersionStatus.Behind(
        builds = newerBuilds,
        atLeast = newerBuilds == publishedBuilds.size && publishedBuilds.size >= PAGE_SIZE,
        latestBuildNumber = latestBuildNumber,
        latestCommit = HEAD_SHA.find(body)?.groupValues?.get(1),
    )
}

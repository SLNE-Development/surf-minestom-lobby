package dev.slne.minestom.lobby.server.version

import com.google.inject.Guice
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LatestReleaseFetcherTest {

    @Test
    fun `same release version is up to date`() {
        val status = parseLatestRelease(
            currentVersion = "1.4.0",
            body = release("v1.4.0"),
        )

        assertEquals(
            LobbyVersionStatus.UpToDate,
            status,
        )
    }

    @Test
    fun `newer minor release is detected`() {
        val status = parseLatestRelease(
            currentVersion = "1.4.0",
            body = release("v1.5.0"),
        )

        assertEquals(
            LobbyVersionStatus.UpdateAvailable("1.5.0"),
            status,
        )
    }

    @Test
    fun `newer patch release is detected`() {
        val status = parseLatestRelease(
            currentVersion = "1.4.0",
            body = release("v1.4.1"),
        )

        assertEquals(
            LobbyVersionStatus.UpdateAvailable("1.4.1"),
            status,
        )
    }

    @Test
    fun `newer major release is detected`() {
        val status = parseLatestRelease(
            currentVersion = "1.9.0",
            body = release("v2.0.0"),
        )

        assertEquals(
            LobbyVersionStatus.UpdateAvailable("2.0.0"),
            status,
        )
    }

    @Test
    fun `running version newer than latest release is treated as up to date`() {
        val status = parseLatestRelease(
            currentVersion = "1.6.0",
            body = release("v1.5.0"),
        )

        assertEquals(
            LobbyVersionStatus.UpToDate,
            status,
        )
    }

    @Test
    fun `invalid release tag fails the check`() {
        val status = parseLatestRelease(
            currentVersion = "1.4.0",
            body = release("latest"),
        )

        assertTrue(
            status is LobbyVersionStatus.CheckFailed,
            "expected a failed check, got $status",
        )
    }

    @Test
    fun `missing release tag fails the check`() {
        val status = parseLatestRelease(
            currentVersion = "1.4.0",
            body = """{"name":"Surf Minestom Lobby"}""",
        )

        assertTrue(
            status is LobbyVersionStatus.CheckFailed,
            "expected a failed check, got $status",
        )
    }

    @Test
    fun `generated build info contains the current project version`() {
        val buildInfo = LobbyBuildInfo.current

        val projectVersion = requireNotNull(System.getProperty("projectVersion"))

        assertEquals(projectVersion, buildInfo.version)

        assertNotNull(
            buildInfo.commit,
            "the commit is missing",
        )

        assertNotNull(
            buildInfo.branch,
            "the branch is missing",
        )

        assertNotNull(
            buildInfo.commitTime,
            "the commit time did not parse",
        )
    }

    @Test
    fun `a jar without a build number is a development build`() = runTest {
        val service = LobbyVersionService(
            LobbyBuildInfo(
                version = "123.456.789",
                commit = OLDER_COMMIT,
                branch = "master",
                commitTime = null,
                buildNumber = null,
            )
        )

        assertEquals(
            LobbyVersionStatus.DevelopmentBuild,
            service.status(),
        )
    }

    @Test
    fun `guice constructs the service from the jar build info`() {
        val service = Guice.createInjector()
            .getInstance(LobbyVersionService::class.java)

        assertEquals(
            LobbyBuildInfo.current,
            service.buildInfo,
        )
    }

    private fun release(tag: String): String =
        """{"tag_name":"$tag","name":"Surf Minestom Lobby $tag"}"""

    private companion object {
        const val OLDER_COMMIT =
            "0123456789abcdef0123456789abcdef01234567"
    }
}
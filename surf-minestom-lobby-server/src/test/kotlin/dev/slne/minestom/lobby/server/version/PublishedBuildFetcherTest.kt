package dev.slne.minestom.lobby.server.version

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PublishedBuildFetcherTest {

    @Test
    fun `a build that no newer run followed is up to date`() {
        val status = parseWorkflowRuns(
            buildNumber = 142,
            body = workflowRuns(142 to LATEST_COMMIT, 141 to OLDER_COMMIT),
        )

        assertEquals(LobbyVersionStatus.UpToDate, status)
    }

    @Test
    fun `only runs newer than the running build are counted`() {
        val status = parseWorkflowRuns(
            buildNumber = 140,
            body = workflowRuns(145 to LATEST_COMMIT, 143 to OLDER_COMMIT, 140 to OLDER_COMMIT),
        )

        val behind = assertBehind(status)
        assertEquals(2, behind.builds)
        assertEquals(145, behind.latestBuildNumber)
        assertEquals(LATEST_COMMIT, behind.latestCommit)
        assertFalse(behind.atLeast, "a partial page is an exact count")
    }

    @Test
    fun `a run that failed to publish leaves no gap in the count`() {
        // The API is asked for successful runs only, so a failed run 144 is simply absent and
        // must not inflate the distance the way `latest - own` would.
        val status = parseWorkflowRuns(
            buildNumber = 143,
            body = workflowRuns(145 to LATEST_COMMIT, 143 to OLDER_COMMIT),
        )

        assertEquals(1, assertBehind(status).builds)
    }

    @Test
    fun `a full page of newer runs reports a lower bound`() {
        val runs = (1..100).map { index -> (1000 - index) to LATEST_COMMIT }

        val behind = assertBehind(
            parseWorkflowRuns(buildNumber = 1, body = workflowRuns(*runs.toTypedArray()))
        )

        assertEquals(100, behind.builds)
        assertTrue(behind.atLeast, "a full page cannot be an exact count")
    }

    @Test
    fun `an empty run list fails the check instead of claiming to be up to date`() {
        val status = parseWorkflowRuns(
            buildNumber = 142,
            body = """{"total_count":0,"workflow_runs":[]}""",
        )

        assertTrue(status is LobbyVersionStatus.CheckFailed, "expected a failed check, got $status")
    }

    @Test
    fun `the generated build info resource survives the properties round-trip`() {
        val buildInfo = LobbyBuildInfo.current

        assertEquals("1.0.0-SNAPSHOT", buildInfo.version)
        assertNotNull(buildInfo.commit, "the commit is missing")
        assertNotNull(buildInfo.branch, "the branch is missing")
        // Gradle escapes the colons of the ISO timestamp, so the parse only works if
        // `Properties.load` unescaped them again.
        assertNotNull(buildInfo.commitTime, "the commit time did not parse")
    }

    @Test
    fun `a jar without a build number is a development build`() = runTest {
        // Tests run against the generated resource of a local build, which carries no build
        // number, so the service must answer without reaching for the network.
        assertEquals(LobbyVersionStatus.DevelopmentBuild, LobbyVersionService().status())
    }

    private fun assertBehind(status: LobbyVersionStatus): LobbyVersionStatus.Behind {
        assertTrue(status is LobbyVersionStatus.Behind, "expected to be behind, got $status")
        return status as LobbyVersionStatus.Behind
    }

    private fun workflowRuns(vararg runs: Pair<Int, String>): String =
        runs.joinToString(
            prefix = """{"total_count":${runs.size},"workflow_runs":[""",
            postfix = "]}",
        ) { (runNumber, headSha) ->
            """{"id":1,"head_branch":"master","head_sha":"$headSha","run_number":$runNumber,""" +
                    """"run_attempt":1,"status":"completed","conclusion":"success"}"""
        }

    private companion object {
        const val LATEST_COMMIT = "f8f3d0e1c2b3a4958677889900aabbccddeeff01"
        const val OLDER_COMMIT = "0123456789abcdef0123456789abcdef01234567"
    }
}

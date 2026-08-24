package dev.slne.minestom.lobby.server.version

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.coroutine.MinestomDispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.TestOnly
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

private val CACHE_DURATION = 15.minutes

/**
 * Reports how far the running build is behind the newest published one, caching the answer for
 * [CACHE_DURATION].
 */
@Singleton
class LobbyVersionService @TestOnly internal constructor(val buildInfo: LobbyBuildInfo) {

    @Inject
    constructor() : this(LobbyBuildInfo.current)

    private val fetcher = PublishedBuildFetcher()
    private val mutex = Mutex()

    private var cached: LobbyVersionStatus? = null
    private var cachedAt: TimeSource.Monotonic.ValueTimeMark? = null

    suspend fun status(): LobbyVersionStatus {
        val buildNumber = buildInfo.buildNumber ?: return LobbyVersionStatus.DevelopmentBuild

        mutex.withLock {
            cachedFresh()?.let { return it }

            val status = withContext(MinestomDispatchers.Blocking) {
                runCatching { fetcher.statusFor(buildNumber) }
                    .getOrElse { failure ->
                        LobbyVersionStatus.CheckFailed(
                            failure.message ?: failure::class.simpleName ?: "unknown error"
                        )
                    }
            }

            cached = status
            cachedAt = TimeSource.Monotonic.markNow()

            return status
        }
    }

    private fun cachedFresh(): LobbyVersionStatus? {
        val mark = cachedAt ?: return null

        return cached?.takeIf { mark.elapsedNow() < CACHE_DURATION }
    }
}

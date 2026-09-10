package dev.slne.minestom.lobby.server.player.visibility

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.player.LobbyPlayerImpl
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import net.minestom.server.MinecraftServer
import net.minestom.server.ServerFlag
import net.minestom.server.entity.AutomaticPlayerVisibility
import net.minestom.server.instance.EntityTracker
import net.minestom.server.instance.Instance
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule

@Singleton
class PlayerVisibilityService @Inject constructor(
    private val settings: ServerConfig.PlayerVisibilityConfig,
) {

    private val selector = NearestPlayerSelector(
        maxVisible = settings.maxVisible,
        activateAt = settings.activateAt,
        deactivateBelow = settings.deactivateBelow,
        retentionDistanceFactor = settings.retentionDistanceFactor,
    )

    private val candidateIds = IntOpenHashSet()

    private var task: Task? = null
    private var phase = 0

    fun start() {
        if (!settings.enabled) return

        check(task == null) {
            "Player visibility service is already running"
        }

        task = MinecraftServer.getSchedulerManager()
            .buildTask(::tick)
            .delay(TaskSchedule.tick(1))
            .repeat(TaskSchedule.tick(1))
            .schedule()
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    private fun tick() {
        val currentPhase = phase

        phase = if (phase + 1 == settings.refreshIntervalTicks) {
            0
        } else {
            phase + 1
        }

        for (player in MinecraftServer.getConnectionManager().onlinePlayers) {
            val viewer = player as? LobbyPlayerImpl ?: continue
            val instance = viewer.instance ?: continue

            if (!viewer.isActive || viewer.isRemoved) continue

            val ruleChanged = viewer.visibilityHandler.applyRequestedRule() ||
                    viewer.visibilityHandler.requiresNativeRuleRefresh

            val previous = viewer.visibilityHandler.visibilitySelection
            val instanceChanged = previous.instance !== instance

            val scheduled = Math.floorMod(
                viewer.entityId,
                settings.refreshIntervalTicks,
            ) == currentPhase

            if (!ruleChanged && !instanceChanged && !scheduled) continue

            try {
                refresh(
                    viewer = viewer,
                    instance = instance,
                    previous = previous,
                    instanceChanged = instanceChanged,
                    ruleChanged = ruleChanged,
                )
            } catch (exception: Exception) {
                MinecraftServer.LOGGER.error(
                    "Failed to refresh player visibility for {}",
                    viewer.username,
                    exception,
                )
            }
        }
    }


    private fun refresh(
        viewer: LobbyPlayerImpl,
        instance: Instance,
        previous: PlayerVisibilityHandler.VisibilitySelection,
        instanceChanged: Boolean,
        ruleChanged: Boolean,
    ) {
        val previousIds = if (instanceChanged) {
            PlayerVisibilityHandler.EMPTY_IDS
        } else {
            previous.ids
        }

        selector.begin(
            previouslyVisible = previousIds,
            wasLimited = viewer.visibilityHandler.visibilityLimited && !instanceChanged,
        )

        candidateIds.clear()

        if (viewer.autoViewEntities()) {
            val position = viewer.position

            instance.entityTracker.nearbyEntitiesByChunkRange(
                position,
                ServerFlag.ENTITY_VIEW_DISTANCE,
                EntityTracker.Target.PLAYERS,
            ) { target ->
                if (
                    target is LobbyPlayer &&
                    target !== viewer &&
                    candidateIds.add(target.entityId) &&
                    target.instance === instance &&
                    target.isActive &&
                    !target.isRemoved &&
                    viewer.visibilityHandler.allowsWithoutBudget(target) &&
                    AutomaticPlayerVisibility.targetAllows(target, viewer)
                ) {
                    selector.offer(
                        target.entityId,
                        position.distanceSquared(target.position),
                    )
                }
            }
        }

        val nextIds = selector.finish()
        viewer.visibilityHandler.visibilityLimited = selector.limited

        viewer.visibilityHandler.pausePlayerAdmission()

        try {
            val oldInstance = previous.instance

            if (oldInstance != null) {
                for (id in previous.ids) {
                    if (instanceChanged || nextIds.binarySearch(id) < 0) {
                        val target = oldInstance.getEntityById(id) as? LobbyPlayer
                            ?: continue

                        AutomaticPlayerVisibility.hide(target, viewer)
                    }
                }
            }

            viewer.visibilityHandler.publishSelection(instance, nextIds)
        } finally {
            viewer.visibilityHandler.resumePlayerAdmission()
        }

        for (id in nextIds) {
            val target = instance.getEntityById(id) as? LobbyPlayer
                ?: continue

            if (withinTrackingRange(viewer, target)) {
                AutomaticPlayerVisibility.show(target, viewer)
            }
        }

        if (ruleChanged) {
            viewer.visibilityHandler.refreshNativeViewerRule()
        }
    }

    private fun withinTrackingRange(
        viewer: LobbyPlayer,
        target: LobbyPlayer,
    ): Boolean {
        val origin = viewer.position
        val position = target.position
        val range = ServerFlag.ENTITY_VIEW_DISTANCE

        return position.chunkX() >= origin.chunkX() - range &&
                position.chunkX() <= origin.chunkX() + range &&
                position.chunkZ() >= origin.chunkZ() - range &&
                position.chunkZ() <= origin.chunkZ() + range
    }
}
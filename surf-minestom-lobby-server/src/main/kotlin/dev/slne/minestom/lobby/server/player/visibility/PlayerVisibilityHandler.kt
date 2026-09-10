package dev.slne.minestom.lobby.server.player.visibility

import dev.slne.minestom.lobby.api.player.LobbyPlayer
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.player.LobbyPlayerImpl
import net.minestom.server.entity.AutomaticPlayerVisibility
import net.minestom.server.entity.Entity
import net.minestom.server.instance.Instance
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Predicate

class PlayerVisibilityHandler(
    private val player: LobbyPlayerImpl,
    private val visibilityConfig: ServerConfig.PlayerVisibilityConfig
) {
    private class RuleRequest(
        val predicate: Predicate<in Entity>?,
    )

    @Suppress("ArrayInDataClass")
    data class VisibilitySelection(
        val instance: Instance?,
        val ids: IntArray,
    )

    companion object {
        val EMPTY_IDS = IntArray(0)
    }

    private val requestedRule = AtomicReference(RuleRequest(null))
    private var appliedRequest = requestedRule.get()

    @Volatile
    private var appliedRule: Predicate<in Entity>? = null

    @Volatile
    private var admittingPlayers = true

    @Volatile
    internal var visibilitySelection = VisibilitySelection(null, EMPTY_IDS)
        private set

    var visibilityLimited = false

    var requiresNativeRuleRefresh = false
        private set

    private val combinedRule = Predicate<Entity> { entity ->
        if (!allowsWithoutBudget(entity)) {
            false
        } else if (entity !is LobbyPlayer) {
            true
        } else {
            val selection = visibilitySelection

            admittingPlayers &&
                    player.autoViewEntities() &&
                    selection.instance === player.instance &&
                    entity.instance === player.instance &&
                    selection.ids.binarySearch(entity.entityId) >= 0
        }
    }

    fun init() {
        if (visibilityConfig.enabled) {
            player.updateViewerRule(combinedRule)
        }
    }

    fun onUpdateViewerRule(
        predicate: Predicate<in Entity>?,
        callSuper: () -> Unit
    ) {
        if (!visibilityConfig.enabled) {
            callSuper()
            return
        }

        requestedRule.set(RuleRequest(predicate))
    }

    fun onUpdateViewerRule(callSuper: () -> Unit) {
        if (!visibilityConfig.enabled) {
            callSuper()
            return
        }

        requestedRule.updateAndGet {
            RuleRequest(it.predicate)
        }
    }

    fun hasPredictableViewers(callSuper: () -> Boolean): Boolean {
        return !visibilityConfig.enabled && callSuper()
    }

    fun allowsWithoutBudget(entity: Entity): Boolean {
        return appliedRule?.test(entity) != false
    }

    fun applyRequestedRule(): Boolean {
        val request = requestedRule.get()
        if (request === appliedRequest) return false

        appliedRequest = request
        appliedRule = request.predicate
        requiresNativeRuleRefresh = true

        return true
    }

    fun pausePlayerAdmission() {
        AutomaticPlayerVisibility.withViewerLock(player) {
            admittingPlayers = false
        }
    }

    fun publishSelection(instance: Instance, ids: IntArray) {
        val previous = visibilitySelection

        if (previous.instance !== instance || previous.ids !== ids) {
            visibilitySelection = VisibilitySelection(instance, ids)
        }
    }

    fun resumePlayerAdmission() {
        AutomaticPlayerVisibility.withViewerLock(player) {
            admittingPlayers = true
        }
    }

    fun refreshNativeViewerRule() {
        if (player.autoViewEntities()) {
            player.`updateViewerRule$super`()
        }

        requiresNativeRuleRefresh = false
    }
}
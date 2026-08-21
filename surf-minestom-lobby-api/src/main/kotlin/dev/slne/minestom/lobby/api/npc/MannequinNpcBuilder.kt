package dev.slne.minestom.lobby.api.npc

import codes.bed.minestom.npc.api.NpcInteraction
import codes.bed.minestom.npc.listener.NpcInteractListener
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.PlayerSkin
import net.minestom.server.instance.Instance
import net.minestom.server.network.player.ResolvableProfile

/**
 * Builds and spawns a [MannequinNpc].
 *
 * ```
 * val npc = mannequinNpc("shop", instance, Pos(0.5, 100.0, 0.5)) {
 *     displayName = Component.text("Shop")
 *     skin = ResolvableProfile(PlayerSkin(textures, signature))
 *     scale = 1.5
 *     onInteract { interaction -> interaction.player.sendMessage("Hello!") }
 * }
 * ```
 */
class MannequinNpcBuilder(private val name: String) {

    /**
     * The hologram text shown above the NPC.
     */
    var displayName: Component = Component.empty()

    /**
     * The profile the mannequin renders, or `null` for the default profile.
     */
    var profile: ResolvableProfile? = null

    /**
     * The size of the mannequin, where `1.0` is player-sized.
     */
    var scale: Double = 1.0

    /**
     * Where the hologram sits relative to the NPC's feet. Defaults to just above the
     * mannequin's head, following [scale].
     */
    var displayNameOffset: Vec? = null

    /**
     * The text the client renders below the mannequin. Empty hides the client's default
     * mannequin label.
     */
    var description: Component = Component.empty()

    private val interactHandlers = mutableListOf<(NpcInteraction) -> Unit>()

    /**
     * Runs [handler] whenever a player clicks the NPC.
     */
    fun onInteract(handler: (NpcInteraction) -> Unit) {
        interactHandlers += handler
    }

    @PublishedApi
    internal fun spawn(instance: Instance, position: Pos): MannequinNpc {
        val npc = MannequinNpc(
            name = name,
            hologramText = displayName,
            profile = profile,
            scale = scale,
            hologramOffset = displayNameOffset ?: Vec(0.0, HOLOGRAM_HEIGHT * scale, 0.0),
            description = description,
        )

        interactHandlers.forEach { handler ->
            npc.onInteract { interaction -> handler(interaction) }
        }

        npc.spawn()
        npc.setInstance(instance, position)

        return npc
    }

    private companion object {
        const val HOLOGRAM_HEIGHT = 2.1
    }
}

/**
 * Creates a [MannequinNpc] named [name], configures it with [block] and spawns it at
 * [position] in [instance].
 */
inline fun mannequinNpc(
    name: String,
    instance: Instance,
    position: Pos,
    block: MannequinNpcBuilder.() -> Unit,
): MannequinNpc = MannequinNpcBuilder(name).apply(block).spawn(instance, position)

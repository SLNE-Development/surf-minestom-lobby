package dev.slne.minestom.lobby.api.npc

import codes.bed.minestom.npc.api.NameDisplayMode
import codes.bed.minestom.npc.api.NpcKind
import codes.bed.minestom.npc.display.TextDisplayController
import codes.bed.minestom.npc.types.AbstractNpcEntity
import dev.slne.minestom.lobby.api.command.entity.editEntityMeta
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.attribute.Attribute
import net.minestom.server.entity.metadata.avatar.MannequinMeta
import net.minestom.server.network.packet.server.play.EntityAttributesPacket
import net.minestom.server.network.player.ResolvableProfile
import org.jetbrains.annotations.ApiStatus
import java.util.*

/**
 * A mannequin-based NPC.
 *
 * Use the [mannequinNpc] builder to create and spawn one.
 *
 * @param name the internal name of the NPC
 * @param hologramText the hologram text shown above the NPC
 * @param profile the profile the mannequin renders, or `null` for the default profile
 * @param scale the size of the mannequin, where `1.0` is player-sized
 * @param hologramOffset where the hologram sits relative to the NPC's feet
 * @param description the text the client renders below the mannequin; empty hides the
 *   client's default mannequin label
 */
class MannequinNpc(
    private val name: String,
    hologramText: Component,
    profile: ResolvableProfile?,
    private val scale: Double,
    hologramOffset: Vec,
    description: Component,
    uuid: UUID = UUID.randomUUID(),
) : AbstractNpcEntity(EntityType.MANNEQUIN, uuid) {

    init {
        editEntityMeta<MannequinMeta> { meta ->
            profile?.let { meta.profile = it }
            meta.isImmovable = true
            meta.description = description
            meta.displayedSkinParts = ALL_SKIN_PARTS
        }
        setNoGravity(true)
        nameDisplayMode = NameDisplayMode.GLOBAL_HOLOGRAM
        textDisplayController = TextDisplayController(hologramText, hologramOffset)
    }

    override val kind: NpcKind get() = NpcKind.MANNEQUIN

    override val displayName: String get() = name

    /**
     * Replaces the hologram text shown above the NPC.
     */
    fun updateDisplayName(hologramText: Component) {
        textDisplayController?.updateText(hologramText)
    }

    @Suppress("UnstableApiUsage")
    @ApiStatus.Internal
    override fun updateNewViewer(player: Player) {
        super.updateNewViewer(player)

        if (scale != 1.0) {
            player.sendPacket(
                EntityAttributesPacket(
                    entityId,
                    listOf(EntityAttributesPacket.Property(Attribute.SCALE, scale, emptyList()))
                )
            )
        }
    }

    companion object {
        private const val ALL_SKIN_PARTS: Byte = 0x7F
    }
}

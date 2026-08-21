package dev.slne.minestom.lobby.server.world.entity

import dev.slne.minestom.lobby.api.command.entity.editEntityMeta
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.minestom.server.MinecraftServer
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.*
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.BlockDisplayMeta
import net.minestom.server.entity.metadata.display.ItemDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.entity.metadata.item.ItemEntityMeta
import net.minestom.server.entity.metadata.other.ArmorStandMeta
import net.minestom.server.entity.metadata.other.EndCrystalMeta
import net.minestom.server.entity.metadata.other.ItemFrameMeta
import net.minestom.server.entity.metadata.other.PaintingMeta
import net.minestom.server.entity.metadata.villager.VillagerMeta
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.registry.StaticProtocolObject
import net.minestom.server.utils.Direction
import net.minestom.server.utils.Rotation

/**
 * Applies one entity's vanilla NBT to a freshly constructed Minestom [Entity].
 */
fun interface EntityNbtMapper {
    fun apply(entity: Entity, nbt: CompoundBinaryTag)
}

/**
 * The per-type mappers, keyed by entity type.
 */
object EntityNbtMappers {

    private val byType = Object2ObjectMap.ofEntries(
        entry(EntityType.ITEM_DISPLAY) { entity, nbt ->
            entity.editEntityMeta<ItemDisplayMeta> { meta ->
                meta.applyDisplay(nbt)

                nbt.compoundOrNull("item")?.let { meta.itemStack = it.toItemStack() }
                nbt.stringOrNull("item_display")
                    ?.toEnumOrNull<ItemDisplayMeta.DisplayContext>()
                    ?.let { meta.displayContext = it }
            }
        },
        entry(EntityType.BLOCK_DISPLAY) { entity, nbt ->
            entity.editEntityMeta<BlockDisplayMeta> { meta ->
                meta.applyDisplay(nbt)

                nbt.compoundOrNull("block_state")?.toBlock()?.let { meta.setBlockState(it) }
            }
        },
        entry(EntityType.TEXT_DISPLAY) { entity, nbt ->
            entity.editEntityMeta<TextDisplayMeta> { meta ->
                meta.applyDisplay(nbt)

                nbt.componentOrNull("text")?.let { meta.text = it }
                nbt.intOrNull("line_width")?.let { meta.lineWidth = it }
                nbt.intOrNull("background")?.let { meta.backgroundColor = it }
                nbt.byteOrNull("text_opacity")?.let { meta.textOpacity = it }
                nbt.booleanOrNull("shadow")?.let { meta.isShadow = it }
                nbt.booleanOrNull("see_through")?.let { meta.isSeeThrough = it }
                nbt.booleanOrNull("default_background")?.let { meta.isUseDefaultBackground = it }
                nbt.stringOrNull("alignment")
                    ?.toEnumOrNull<TextDisplayMeta.Alignment>()
                    ?.let { meta.alignment = it }
            }
        },
        entry(EntityType.ARMOR_STAND) { entity, nbt ->
            entity.editEntityMeta<ArmorStandMeta> { meta ->
                nbt.booleanOrNull("Small")?.let { meta.isSmall = it }
                nbt.booleanOrNull("ShowArms")?.let { meta.isHasArms = it }
                nbt.booleanOrNull("NoBasePlate")?.let { meta.isHasNoBasePlate = it }
                nbt.booleanOrNull("Marker")?.let { meta.isMarker = it }

                nbt.compoundOrNull("Pose")?.let { pose ->
                    pose.vecOrNull("Head")?.let { meta.headRotation = it }
                    pose.vecOrNull("Body")?.let { meta.bodyRotation = it }
                    pose.vecOrNull("LeftArm")?.let { meta.leftArmRotation = it }
                    pose.vecOrNull("RightArm")?.let { meta.rightArmRotation = it }
                    pose.vecOrNull("LeftLeg")?.let { meta.leftLegRotation = it }
                    pose.vecOrNull("RightLeg")?.let { meta.rightLegRotation = it }
                }
            }
        },
        entry(EntityType.ITEM_FRAME) { entity, nbt ->
            entity.applyItemFrame(nbt)
        },
        entry(EntityType.GLOW_ITEM_FRAME) { entity, nbt ->
            entity.applyItemFrame(nbt)
        },
        entry(EntityType.PAINTING) { entity, nbt ->
            nbt.stringOrNull("variant")
                ?.let { MinecraftServer.getPaintingVariantRegistry().getKey(Key.key(it)) }
                ?.let { entity.set(DataComponents.PAINTING_VARIANT, it) }

            entity.editEntityMeta<PaintingMeta> { meta ->
                nbt.byteOrNull("facing")
                    ?.let { Direction.HORIZONTAL.getOrNull(it.toInt()) }
                    ?.let { meta.direction = it }
            }
        },
        entry(EntityType.ITEM) { entity, nbt ->
            entity.editEntityMeta<ItemEntityMeta> { meta ->
                nbt.compoundOrNull("Item")?.let { meta.item = it.toItemStack() }
            }
        },
        entry(EntityType.VILLAGER) { entity, nbt ->
            entity.editEntityMeta<VillagerMeta> { meta ->
                val data = nbt.compoundOrNull("VillagerData") ?: return@editEntityMeta
                val default = meta.villagerData ?: VillagerMeta.VillagerData.DEFAULT

                val level = data.intOrNull("level")
                    ?.let { VillagerMeta.Level.entries.getOrNull(it - 1) }

                meta.villagerData = VillagerMeta.VillagerData(
                    data.stringOrNull("type")?.toStaticKey(VillagerType.entries)
                        ?: default.type(),
                    data.stringOrNull("profession")
                        ?.let { VillagerProfession.fromKey(Key.key(it)) }
                        ?: default.profession(),
                    level ?: default.level()
                )
            }
        },
        entry(EntityType.CAT) { entity, nbt ->
            nbt.stringOrNull("variant")
                ?.let { MinecraftServer.getCatVariantRegistry().getKey(Key.key(it)) }
                ?.let { entity.set(DataComponents.CAT_VARIANT, it) }
        },
        entry(EntityType.END_CRYSTAL) { entity, nbt ->
            entity.editEntityMeta<EndCrystalMeta> { meta ->
                nbt.booleanOrNull("ShowBottom")
                    ?.let { meta.isShowingBottom = it }
            }
        }
    )

    operator fun get(type: EntityType): EntityNbtMapper? = byType[type]

    private fun entry(type: EntityType, mapper: EntityNbtMapper) =
        Object2ObjectMap.entry(type, mapper)
}

fun Entity.applyCommon(nbt: CompoundBinaryTag) {
    nbt.componentOrNull("CustomName")?.let { set(DataComponents.CUSTOM_NAME, it) }
    nbt.booleanOrNull("CustomNameVisible")?.let { isCustomNameVisible = it }
    nbt.booleanOrNull("NoGravity")?.let { setNoGravity(it) }
    nbt.booleanOrNull("Silent")?.let { isSilent = it }
    nbt.booleanOrNull("Glowing")?.let { isGlowing = it }
    nbt.booleanOrNull("Invisible")?.let { isInvisible = it }

    nbt.vecOrNull("Motion")?.let { velocity = it }

    if (this is LivingEntity) {
        nbt.floatOrNull("Health")?.let { health = it }
        nbt.compoundOrNull("equipment")?.let { applyEquipment(it) }
    }
}

private fun LivingEntity.applyEquipment(equipment: CompoundBinaryTag) {
    for (slot in EquipmentSlot.entries) {
        val item = equipment.compoundOrNull(slot.nbtName()) ?: continue

        setEquipment(slot, item.toItemStack())
    }
}

private fun Entity.applyItemFrame(nbt: CompoundBinaryTag) {
    editEntityMeta(ItemFrameMeta::class.java) { meta ->
        nbt.compoundOrNull("Item")?.let { meta.item = it.toItemStack() }
        nbt.byteOrNull("ItemRotation")
            ?.let { Rotation.entries.getOrNull(it.toInt()) }
            ?.let { meta.rotation = it }

        nbt.byteOrNull("Facing")
            ?.let { Direction.entries.getOrNull(it.toInt()) }
            ?.let { meta.direction = it }
    }
}

private fun <T : StaticProtocolObject<T>> String.toStaticKey(values: List<T>): T? =
    values.firstOrNull { it.key().asString() == this }

private fun AbstractDisplayMeta.applyDisplay(nbt: CompoundBinaryTag) {
    nbt.compoundOrNull("transformation")?.let { transformation ->
        transformation.vecOrNull("translation")?.let { translation = it }
        transformation.vecOrNull("scale")?.let { scale = it }
        transformation.quaternionOrNull("left_rotation")?.let { leftRotation = it }
        transformation.quaternionOrNull("right_rotation")?.let { rightRotation = it }
    }

    nbt.stringOrNull("billboard")
        ?.toEnumOrNull<AbstractDisplayMeta.BillboardConstraints>()
        ?.let { billboardRenderConstraints = it }

    nbt.floatOrNull("view_range")?.let { viewRange = it }
    nbt.floatOrNull("shadow_radius")?.let { shadowRadius = it }
    nbt.floatOrNull("shadow_strength")?.let { shadowStrength = it }
    nbt.floatOrNull("width")?.let { width = it }
    nbt.floatOrNull("height")?.let { height = it }
    nbt.intOrNull("glow_color_override")?.let { glowColorOverride = it }
    nbt.intOrNull("interpolation_duration")?.let { transformationInterpolationDuration = it }
    nbt.intOrNull("start_interpolation")?.let { transformationInterpolationStartDelta = it }
    nbt.intOrNull("teleport_duration")?.let { posRotInterpolationDuration = it }

    nbt.compoundOrNull("brightness")?.let { brightness ->
        setBrightness(brightness.intOrNull("block") ?: 0, brightness.intOrNull("sky") ?: 0)
    }
}

private fun CompoundBinaryTag.toItemStack(): ItemStack =
    ItemStack.fromItemNBT(this, MinecraftServer.getRegistries())

private fun CompoundBinaryTag.toBlock(): Block? {
    val block = stringOrNull("Name")?.let { Block.fromKey(it) } ?: return null
    val properties = compoundOrNull("Properties") ?: return block

    return block.withProperties(properties.keySet().associateWith { properties.getString(it) })
}

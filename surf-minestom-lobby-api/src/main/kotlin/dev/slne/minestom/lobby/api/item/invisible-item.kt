package dev.slne.minestom.lobby.api.item

import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

/**
 * The item model the resource pack renders as fully transparent.
 */
const val INVISIBLE_ITEM_MODEL = "nexo:invisible_item"

/**
 * Creates an item without a visible texture.
 *
 * Such items act as label-only, clickable elements in menus and hotbars: the client renders
 * no icon, while the display name and lore still show as usual.
 */
fun invisibleItem(): ItemStack = ItemStack.builder(Material.PAPER)
    .set(DataComponents.ITEM_MODEL, INVISIBLE_ITEM_MODEL)
    .build()

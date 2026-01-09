package com.vaultpack.gui;

import com.vaultpack.models.PlayerBackpackData;
import com.vaultpack.utils.ItemBuilderUtil;
import com.vaultpack.managers.NavigationHandler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Phase 3: GUI builder for backpack navigation headers
 * Extracted from BackpackManager for better separation of concerns
 */
public class BackpackGUIBuilder {

    // Player head textures for navigation buttons
    private static final String FIRST_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWQ3MDdkYjQ2YTVhY2JmZWJmNjEyMzk1MzZkMjU2NDgxMzRiYjQzYjY1YzE2NzE2YmEzMjljNmRiZjQxMiJ9fX0=";
    private static final String PREVIOUS_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTVkYTQ4NDcyNzI1ODIyNjViZGFjYTM2NzIzN2M5NjEyMmIxMzlmNGU1OTdmYmM2NjY3ZDNmYjc1ZmVhN2NmNiJ9fX0=";
    private static final String NEXT_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjUyN2ViYWU5ZjE1MzE1NGE3ZWQ0OWM4OGMwMmI1YTlhOWNhN2NiMTYxOGQ5OTE0YTNkOWRmOGNjYjNjODQifX19";
    private static final String LAST_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWI3MThhYmUxMDI0NzYyYzFjNTIyNWM5YjNjZjk0M2EwMTRmNTRkZDhkNGQ0NjRhMmQ2MWYwOThjMDdkOWUifX19";

    /**
     * Add navigation header to backpack inventory
     * Creates a row of buttons at the top for navigation and management
     *
     * @param inventory The inventory to add the header to
     * @param player The player viewing the backpack
     * @param currentSlot The current backpack slot number
     * @param data The player's backpack data
     */
    public static void addNavigationHeader(Inventory inventory, Player player, int currentSlot, PlayerBackpackData data) {
        // Slot 0: Close button
        inventory.setItem(0, ItemBuilderUtil.createButton(
            Material.BARRIER,
            "&c&lClose",
            "&7Click to close this backpack"
        ));

        // Slot 1: Back to all backpacks
        inventory.setItem(1, ItemBuilderUtil.createButton(
            Material.CHEST,
            "&e&lAll Backpacks",
            "&7Return to backpack menu"
        ));

        // Slots 2, 3, 4: Filler (black glass pane to prevent item placement)
        ItemStack filler = ItemBuilderUtil.createButton(Material.BLACK_STAINED_GLASS_PANE, " ");
        inventory.setItem(2, filler);
        inventory.setItem(3, filler);
        inventory.setItem(4, filler);

        // Find first and last backpack slots
        int firstSlot = NavigationHandler.findFirstBackpack(data);
        int lastSlot = NavigationHandler.findLastBackpack(data);

        // Slot 5: First backpack
        if (firstSlot != -1 && firstSlot != currentSlot) {
            inventory.setItem(5, ItemBuilderUtil.createButtonWithTexture(
                FIRST_TEXTURE,
                "&a&lFirst Backpack",
                "&7Jump to backpack #" + firstSlot
            ));
        } else {
            inventory.setItem(5, ItemBuilderUtil.createButton(
                Material.GRAY_DYE,
                "&7First Backpack",
                "&cAlready at first or only backpack"
            ));
        }

        // Slot 6: Previous backpack
        int previousSlot = NavigationHandler.findPreviousBackpack(data, currentSlot);
        if (previousSlot != -1) {
            inventory.setItem(6, ItemBuilderUtil.createButtonWithTexture(
                PREVIOUS_TEXTURE,
                "&e&lPrevious",
                "&7Go to backpack #" + previousSlot
            ));
        } else {
            inventory.setItem(6, ItemBuilderUtil.createButton(
                Material.GRAY_DYE,
                "&7Previous",
                "&cNo previous backpack"
            ));
        }

        // Slot 7: Next backpack
        int nextSlot = NavigationHandler.findNextBackpack(data, currentSlot);
        if (nextSlot != -1) {
            inventory.setItem(7, ItemBuilderUtil.createButtonWithTexture(
                NEXT_TEXTURE,
                "&e&lNext",
                "&7Go to backpack #" + nextSlot
            ));
        } else {
            inventory.setItem(7, ItemBuilderUtil.createButton(
                Material.GRAY_DYE,
                "&7Next",
                "&cNo next backpack"
            ));
        }

        // Slot 8: Last backpack
        if (lastSlot != -1 && lastSlot != currentSlot) {
            inventory.setItem(8, ItemBuilderUtil.createButtonWithTexture(
                LAST_TEXTURE,
                "&a&lLast Backpack",
                "&7Jump to backpack #" + lastSlot
            ));
        } else {
            inventory.setItem(8, ItemBuilderUtil.createButton(
                Material.GRAY_DYE,
                "&7Last Backpack",
                "&cAlready at last or only backpack"
            ));
        }
    }

    /**
     * Create a filler item (typically glass pane)
     * Used to fill empty slots and prevent item placement
     *
     * @param material The material to use for the filler
     * @param name The display name (can be empty string for no name)
     * @return Filler ItemStack
     */
    public static ItemStack createFiller(Material material, String name) {
        return ItemBuilderUtil.createButton(material, name);
    }

    /**
     * Create a standard close button
     *
     * @return Close button ItemStack
     */
    public static ItemStack createCloseButton() {
        return ItemBuilderUtil.createButton(
            Material.BARRIER,
            "&c&lClose",
            "&7Click to close"
        );
    }

    /**
     * Create a standard back button
     *
     * @param destination Description of where the button goes back to
     * @return Back button ItemStack
     */
    public static ItemStack createBackButton(String destination) {
        return ItemBuilderUtil.createButton(
            Material.ARROW,
            "&e&lBack",
            "&7Return to " + destination
        );
    }
}

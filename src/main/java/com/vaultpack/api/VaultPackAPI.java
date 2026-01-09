package com.vaultpack.api;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.data.holders.PlayerDataHolder;
import com.vaultpack.models.Backpack;
import com.vaultpack.models.BackpackTier;
import com.vaultpack.models.EnderPage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

/**
 * Main API class for VaultPack
 * This provides a safe, stable interface for other plugins to interact with VaultPack
 *
 * Usage example:
 * VaultPackAPI api = VaultPackAPI.getInstance();
 * if (api.hasBackpack(player, 1)) {
 *     api.openBackpack(player, 1);
 * }
 *
 * @version 3.0.0
 */
public class VaultPackAPI {

    private static VaultPackAPI instance;
    private final VaultPackPlugin plugin;
    private final VaultPackAPIImpl apiImpl;

    private VaultPackAPI(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.apiImpl = new VaultPackAPIImpl(plugin);
    }

    /**
     * Initialize the API (called internally by VaultPack)
     */
    public static void initialize(VaultPackPlugin plugin) {
        if (instance == null) {
            instance = new VaultPackAPI(plugin);
        }
    }

    /**
     * Get the API instance
     * @return VaultPackAPI instance, or null if not initialized
     */
    public static VaultPackAPI getInstance() {
        return instance;
    }

    /**
     * Get the backpack API
     * @return BackpackAPI instance
     * @since 3.0.0
     */
    public BackpackAPI getBackpackAPI() {
        return apiImpl.getBackpackAPI();
    }

    /**
     * Get the ender chest API
     * @return EnderChestAPI instance
     * @since 3.0.0
     */
    public EnderChestAPI getEnderChestAPI() {
        return apiImpl.getEnderChestAPI();
    }

    // ========== Backpack Management ==========

    /**
     * Check if a player has a backpack in a specific slot
     * @param player The player to check
     * @param slotNumber The slot number (1-18)
     * @return true if the player has a backpack in that slot
     */
    public boolean hasBackpack(Player player, int slotNumber) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        return data.hasBackpack(slotNumber);
    }

    /**
     * Check if a player has a backpack in a specific slot by UUID
     * @param uuid The player's UUID
     * @param slotNumber The slot number (1-18)
     * @return true if the player has a backpack in that slot
     */
    public boolean hasBackpack(UUID uuid, int slotNumber) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(uuid);
        return data.hasBackpack(slotNumber);
    }

    /**
     * Open a player's backpack
     * @param player The player opening the backpack
     * @param slotNumber The slot number (1-18)
     */
    public void openBackpack(Player player, int slotNumber) {
        plugin.getBackpackManager().openBackpack(player, slotNumber);
    }

    /**
     * Open the main backpack menu for a player
     * @param player The player to open the menu for
     */
    public void openBackpackMenu(Player player) {
        plugin.getBackpackManager().openBackpackMenu(player);
    }

    /**
     * Create a backpack in a specific slot
     * @param player The player to create the backpack for
     * @param slotNumber The slot number (1-18)
     * @param tier The backpack tier
     */
    public void createBackpack(Player player, int slotNumber, BackpackTier tier) {
        plugin.getBackpackManager().createBackpack(player, slotNumber, tier);
    }

    /**
     * Get the contents of a backpack
     * @param player The player who owns the backpack
     * @param slotNumber The slot number (1-18)
     * @return Map of slot index to ItemStack, or null if no backpack exists
     */
    public Map<Integer, ItemStack> getBackpackContents(Player player, int slotNumber) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        Backpack backpack = data.getBackpack(slotNumber);
        return backpack != null ? backpack.getContents() : null;
    }

    /**
     * Get the contents of a backpack by UUID
     * @param uuid The player's UUID
     * @param slotNumber The slot number (1-18)
     * @return Map of slot index to ItemStack, or null if no backpack exists
     */
    public Map<Integer, ItemStack> getBackpackContents(UUID uuid, int slotNumber) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(uuid);
        Backpack backpack = data.getBackpack(slotNumber);
        return backpack != null ? backpack.getContents() : null;
    }

    /**
     * Get the size of a backpack (in slots)
     * @param player The player who owns the backpack
     * @param slotNumber The slot number (1-18)
     * @return The size in slots, or 0 if no backpack exists
     */
    public int getBackpackSize(Player player, int slotNumber) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        Backpack backpack = data.getBackpack(slotNumber);
        return backpack != null ? backpack.getSize() : 0;
    }

    /**
     * Get the tier of a backpack
     * @param player The player who owns the backpack
     * @param slotNumber The slot number (1-18)
     * @return The BackpackTier, or null if no backpack exists
     */
    public BackpackTier getBackpackTier(Player player, int slotNumber) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        Backpack backpack = data.getBackpack(slotNumber);
        return backpack != null ? backpack.getTier() : null;
    }

    // ========== Slot Management ==========

    /**
     * Check if a player has a slot unlocked
     * @param player The player to check
     * @param slotNumber The slot number (1-18)
     * @return true if the slot is unlocked
     */
    public boolean isSlotUnlocked(Player player, int slotNumber) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        return data.isSlotUnlocked(slotNumber);
    }

    /**
     * Unlock a slot for a player (bypasses economy/permissions)
     * @param player The player to unlock the slot for
     * @param slotNumber The slot number (1-18)
     */
    public void unlockSlot(Player player, int slotNumber) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        data.unlockSlot(slotNumber);
        plugin.getDataManager().savePlayerData(player.getUniqueId());
    }

    /**
     * Get the number of unlocked slots for a player
     * @param player The player to check
     * @return The number of unlocked slots
     */
    public int getUnlockedSlots(Player player) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        return data.getUnlockedSlots();
    }

    // ========== Statistics ==========

    /**
     * Get the number of active backpacks a player has
     * @param player The player to check
     * @return The number of backpacks placed in slots
     */
    public int getActiveBackpackCount(Player player) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        return data.getActiveBackpackCount();
    }

    /**
     * Get the total storage slots available to a player (across all backpacks)
     * @param player The player to check
     * @return The total number of storage slots
     */
    public int getTotalStorageSlots(Player player) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        return data.getTotalStorageSlots();
    }

    /**
     * Get the total number of used slots across all backpacks
     * @param player The player to check
     * @return The number of used slots
     */
    public int getTotalUsedSlots(Player player) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        return data.getTotalUsedSlots();
    }

    // ========== Ender Chest Management (v2.0.0) ==========

    /**
     * Check if a player has an ender page unlocked
     * @param player The player to check
     * @param pageNumber The page number (1-9)
     * @return true if the ender page is unlocked
     */
    public boolean isEnderPageUnlocked(Player player, int pageNumber) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        return data.isEnderPageUnlocked(pageNumber);
    }

    /**
     * Check if a player has an ender page unlocked by UUID
     * @param uuid The player's UUID
     * @param pageNumber The page number (1-9)
     * @return true if the ender page is unlocked
     */
    public boolean isEnderPageUnlocked(UUID uuid, int pageNumber) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(uuid);
        return data.isEnderPageUnlocked(pageNumber);
    }

    /**
     * Get the number of unlocked ender pages for a player
     * @param player The player to check
     * @return The number of unlocked ender pages
     */
    public int getUnlockedEnderPages(Player player) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        return data.getUnlockedEnderPages();
    }

    /**
     * Unlock an ender page for a player (bypasses economy/permissions)
     * @param player The player to unlock the page for
     * @param pageNumber The page number (1-9)
     */
    public void unlockEnderPage(Player player, int pageNumber) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        data.unlockEnderPage(pageNumber);
        plugin.getDataManager().savePlayerData(player.getUniqueId());
    }

    /**
     * Open an ender page for a player
     * @param player The player to open the page for
     * @param pageNumber The page number (1-9)
     */
    public void openEnderPage(Player player, int pageNumber) {
        plugin.getEnderChestManager().openEnderPage(player, pageNumber);
    }

    /**
     * Get the contents of an ender page
     * @param player The player who owns the ender page
     * @param pageNumber The page number (1-9)
     * @return Map of slot index to ItemStack, or null if page doesn't exist
     */
    public Map<Integer, ItemStack> getEnderPageContents(Player player, int pageNumber) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        EnderPage enderPage = data.getEnderPage(pageNumber);
        return enderPage != null ? enderPage.getContents() : null;
    }

    /**
     * Get the contents of an ender page by UUID
     * @param uuid The player's UUID
     * @param pageNumber The page number (1-9)
     * @return Map of slot index to ItemStack, or null if page doesn't exist
     */
    public Map<Integer, ItemStack> getEnderPageContents(UUID uuid, int pageNumber) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(uuid);
        EnderPage enderPage = data.getEnderPage(pageNumber);
        return enderPage != null ? enderPage.getContents() : null;
    }

    /**
     * Get the total ender chest storage slots available to a player
     * @param player The player to check
     * @return The total number of ender storage slots (unlocked pages * 45)
     */
    public int getTotalEnderStorageSlots(Player player) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        return data.getTotalEnderStorageSlots();
    }

    /**
     * Get the total number of used slots across all ender pages
     * @param player The player to check
     * @return The number of used ender slots
     */
    public int getTotalUsedEnderSlots(Player player) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        return data.getTotalUsedEnderSlots();
    }

    /**
     * Open the unified storage GUI (both backpacks and ender chest)
     * @param player The player to open the GUI for
     */
    public void openUnifiedStorageGUI(Player player) {
        plugin.getBackpackManager().openUnifiedStorageGUI(player);
    }

    // ========== Configuration ==========

    /**
     * Check if a material is blacklisted from backpacks
     * @param material The material to check
     * @return true if the material is blacklisted
     */
    public boolean isBlacklisted(org.bukkit.Material material) {
        return plugin.getConfigManager().isBlacklisted(material);
    }

    /**
     * Get the maximum number of backpack slots
     * @return The maximum slots configured
     */
    public int getMaxBackpackSlots() {
        return plugin.getConfigManager().getMaxBackpackSlots();
    }

    // ========== Version Info ==========

    /**
     * Get the VaultPack plugin version
     * @return The version string
     */
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * Get the VaultPack plugin instance (for advanced usage)
     * @return The VaultPackPlugin instance
     */
    public VaultPackPlugin getPlugin() {
        return plugin;
    }
}

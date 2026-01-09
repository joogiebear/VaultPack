package com.vaultpack.api;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.data.holders.PlayerDataHolder;
import com.vaultpack.models.Backpack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of BackpackAPI.
 * Internal use only - external plugins should use the interface.
 */
public class BackpackAPIImpl implements BackpackAPI {

    private final VaultPackPlugin plugin;

    public BackpackAPIImpl(VaultPackPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public int getUnlockedSlots(UUID playerId) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(playerId);
        return data.getUnlockedSlots();
    }

    @Override
    public boolean unlockSlot(UUID playerId, int slot) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(playerId);
        data.unlockSlot(slot);
        plugin.getDataManager().savePlayerData(playerId);
        return true;
    }

    @Override
    public boolean isSlotUnlocked(UUID playerId, int slot) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(playerId);
        return data.isSlotUnlocked(slot);
    }

    @Override
    public boolean hasBackpack(UUID playerId, int slot) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(playerId);
        return data.hasBackpack(slot);
    }

    @Override
    public Optional<Backpack> getBackpack(UUID playerId, int slot) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(playerId);
        return Optional.ofNullable(data.getBackpack(slot));
    }

    @Override
    public boolean openBackpack(Player player, int slot) {
        plugin.getBackpackManager().openBackpack(player, slot);
        return true;
    }

    @Override
    public int getActiveBackpackCount(UUID playerId) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(playerId);
        return data.getActiveBackpackCount();
    }

    @Override
    public int getTotalStorageSlots(UUID playerId) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(playerId);
        return data.getTotalStorageSlots();
    }

    @Override
    public int getTotalUsedSlots(UUID playerId) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(playerId);
        return data.getTotalUsedSlots();
    }

    @Override
    public boolean addItem(UUID playerId, int slot, ItemStack item) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(playerId);
        Backpack backpack = data.getBackpack(slot);
        
        if (backpack == null) {
            return false;
        }

        // Try to add item to backpack
        for (int i = 0; i < backpack.getSize(); i++) {
            if (!backpack.getContents().containsKey(i)) {
                backpack.getContents().put(i, item);
                plugin.getDataManager().savePlayerData(playerId);
                return true;
            }
        }

        return false;
    }

    @Override
    public int getMaxBackpackSlots() {
        return plugin.getConfigManager().getMaxBackpackSlots();
    }
}

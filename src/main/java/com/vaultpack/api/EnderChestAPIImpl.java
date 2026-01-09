package com.vaultpack.api;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.data.holders.PlayerDataHolder;
import com.vaultpack.models.EnderPage;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of EnderChestAPI.
 * Internal use only - external plugins should use the interface.
 */
public class EnderChestAPIImpl implements EnderChestAPI {

    private final VaultPackPlugin plugin;

    public EnderChestAPIImpl(VaultPackPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public int getUnlockedPages(UUID playerId) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(playerId);
        return data.getUnlockedEnderPages();
    }

    @Override
    public boolean unlockPage(UUID playerId, int page) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(playerId);
        data.unlockEnderPage(page);
        plugin.getDataManager().savePlayerData(playerId);
        return true;
    }

    @Override
    public boolean isPageUnlocked(UUID playerId, int page) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(playerId);
        return data.isEnderPageUnlocked(page);
    }

    @Override
    public Optional<EnderPage> getPage(UUID playerId, int page) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(playerId);
        return Optional.ofNullable(data.getEnderPage(page));
    }

    @Override
    public boolean openEnderPage(Player player, int page) {
        plugin.getEnderChestManager().openEnderPage(player, page);
        return true;
    }

    @Override
    public int getTotalStorageSlots(UUID playerId) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(playerId);
        return data.getTotalEnderStorageSlots();
    }

    @Override
    public int getTotalUsedSlots(UUID playerId) {
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(playerId);
        return data.getTotalUsedEnderSlots();
    }
}

package com.vaultpack.placeholder;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.models.Backpack;
import com.vaultpack.models.PlayerBackpackData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class BackpackPlaceholder extends PlaceholderExpansion {

    private final VaultPackPlugin plugin;

    public BackpackPlaceholder(VaultPackPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "vaultpack";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "Joogiebear";
    }

    @Override
    @NotNull
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        // %ecobackpack_total_slots%
        if (params.equals("total_slots")) {
            return String.valueOf(data.getUnlockedSlots());
        }

        // %ecobackpack_active_count%
        if (params.equals("active_count")) {
            return String.valueOf(data.getActiveBackpackCount());
        }

        // %ecobackpack_total_storage%
        if (params.equals("total_storage")) {
            return String.valueOf(data.getTotalStorageSlots());
        }

        // %ecobackpack_slot_X_item% - Returns material for slot (gray_dye, lime_dye, or chest)
        if (params.startsWith("slot_") && params.endsWith("_item")) {
            String[] parts = params.split("_");
            if (parts.length >= 2) {
                try {
                    int slot = Integer.parseInt(parts[1]);
                    return getSlotItem(data, slot);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // %ecobackpack_slot_X_name% - Returns display name for slot
        if (params.startsWith("slot_") && params.endsWith("_name")) {
            String[] parts = params.split("_");
            if (parts.length >= 2) {
                try {
                    int slot = Integer.parseInt(parts[1]);
                    return getSlotName(data, slot);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // %ecobackpack_slot_X_unlocked% - true/false
        if (params.startsWith("slot_") && params.endsWith("_unlocked")) {
            String[] parts = params.split("_");
            if (parts.length >= 2) {
                try {
                    int slot = Integer.parseInt(parts[1]);
                    return String.valueOf(data.isSlotUnlocked(slot));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // %ecobackpack_slot_X_has_backpack% - true/false
        if (params.startsWith("slot_") && params.endsWith("_has_backpack")) {
            String[] parts = params.split("_");
            if (parts.length >= 2) {
                try {
                    int slot = Integer.parseInt(parts[1]);
                    return String.valueOf(data.hasBackpack(slot));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // %ecobackpack_slot_X_tier% - Tier name
        if (params.startsWith("slot_") && params.endsWith("_tier")) {
            String[] parts = params.split("_");
            if (parts.length >= 2) {
                try {
                    int slot = Integer.parseInt(parts[1]);
                    Backpack backpack = data.getBackpack(slot);
                    return backpack != null ? backpack.getTier().getDisplayName() : "None";
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // %ecobackpack_slot_X_size% - Size in slots
        if (params.startsWith("slot_") && params.endsWith("_size")) {
            String[] parts = params.split("_");
            if (parts.length >= 2) {
                try {
                    int slot = Integer.parseInt(parts[1]);
                    Backpack backpack = data.getBackpack(slot);
                    return backpack != null ? String.valueOf(backpack.getSize()) : "0";
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // %ecobackpack_slot_X_used% - Used slots
        if (params.startsWith("slot_") && params.endsWith("_used")) {
            String[] parts = params.split("_");
            if (parts.length >= 2) {
                try {
                    int slot = Integer.parseInt(parts[1]);
                    Backpack backpack = data.getBackpack(slot);
                    return backpack != null ? String.valueOf(backpack.getUsedSlots()) : "0";
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // %ecobackpack_slot_X_fullness% - Fullness percentage
        if (params.startsWith("slot_") && params.endsWith("_fullness")) {
            String[] parts = params.split("_");
            if (parts.length >= 2) {
                try {
                    int slot = Integer.parseInt(parts[1]);
                    Backpack backpack = data.getBackpack(slot);
                    return backpack != null ? String.format("%.1f%%", backpack.getFullnessPercent()) : "0%";
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // %ecobackpack_slot_X_fullness_bar% - Fullness bar
        if (params.startsWith("slot_") && params.endsWith("_fullness_bar")) {
            String[] parts = params.split("_");
            if (parts.length >= 2) {
                try {
                    int slot = Integer.parseInt(parts[1]);
                    Backpack backpack = data.getBackpack(slot);
                    return backpack != null ? backpack.getFullnessBar() : "&8[----------]";
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return null;
    }

    private String getSlotItem(PlayerBackpackData data, int slot) {
        if (!data.isSlotUnlocked(slot)) {
            return "gray_dye"; // Locked
        }

        if (!data.hasBackpack(slot)) {
            return "lime_dye"; // Empty/unlocked
        }

        return "chest"; // Has backpack
    }

    private String getSlotName(PlayerBackpackData data, int slot) {
        if (!data.isSlotUnlocked(slot)) {
            int cost = plugin.getConfigManager().getSlotUnlockCost(slot);
            String perm = plugin.getConfigManager().getSlotPermission(slot);
            return ChatColor.translateAlternateColorCodes('&',
                    "&7Backpack Slot #" + slot + " &c[LOCKED]");
        }

        if (!data.hasBackpack(slot)) {
            return ChatColor.translateAlternateColorCodes('&',
                    "&aBackpack Slot #" + slot + " &7[EMPTY]");
        }

        Backpack backpack = data.getBackpack(slot);
        return ChatColor.translateAlternateColorCodes('&',
                "&6Backpack #" + slot);
    }
}

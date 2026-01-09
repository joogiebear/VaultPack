package com.vaultpack.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.vaultpack.VaultPackPlugin;
import com.vaultpack.models.PlayerBackpackData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * ACF-based main admin command for VaultPack.
 * Handles reload, give, inspect, and other administrative functions.
 */
@CommandAlias("vaultpack|vp")
@Description("VaultPack administration commands")
public class VaultPackCommand extends BaseCommand {

    private final VaultPackPlugin plugin;

    public VaultPackCommand(VaultPackPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Default command - shows help.
     * Usage: /vaultpack
     */
    @Default
    @CatchUnknown
    @Description("Show VaultPack help")
    public void onDefault(CommandSender sender) {
        sendHelp(sender);
    }

    /**
     * Show help information.
     * Usage: /vaultpack help
     */
    @Subcommand("help")
    @Description("Show VaultPack help")
    @HelpCommand
    public void onHelp(CommandSender sender) {
        sendHelp(sender);
    }

    /**
     * Show plugin version and info.
     * Usage: /vaultpack version
     */
    @Subcommand("version|ver|info")
    @Description("Show plugin version and information")
    public void onVersion(CommandSender sender) {
        List<String> versionMessages = plugin.getMessageManager().getMessageList("admin.version");
        for (String message : versionMessages) {
            String formatted = message
                .replace("%version%", plugin.getDescription().getVersion())
                .replace("%vault%", plugin.isVaultEnabled() ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗")
                .replace("%papi%", plugin.isPlaceholderAPIEnabled() ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗");
            sender.sendMessage(formatted);
        }
    }

    /**
     * Reload plugin configuration.
     * Usage: /vaultpack reload
     */
    @Subcommand("reload")
    @CommandPermission("vaultpack.admin")
    @Description("Reload plugin configuration")
    public void onReload(CommandSender sender) {
        plugin.getMessageManager().send(sender, "admin-reload-start");
        plugin.reload();

        List<String> reloadMessages = plugin.getMessageManager().getMessageList("admin.reload-success");
        for (String message : reloadMessages) {
            sender.sendMessage(message.replace("%version%", plugin.getDescription().getVersion()));
        }
    }

    /**
     * Give a player a backpack slot.
     * Usage: /vaultpack give <player> <slot>
     */
    @Subcommand("give")
    @CommandPermission("vaultpack.admin")
    @Description("Give a player a backpack slot")
    @CommandCompletion("@players @backpackSlots")
    @Syntax("<player> <slot>")
    public void onGive(CommandSender sender, Player target, int slot) {
        if (slot < 1 || slot > plugin.getConfigManager().getMaxBackpackSlots()) {
            plugin.getMessageManager().send(sender, "invalid-slot",
                "%max%", String.valueOf(plugin.getConfigManager().getMaxBackpackSlots()));
            return;
        }

        PlayerBackpackData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        data.unlockSlot(slot);
        plugin.getDataManager().savePlayerData(target.getUniqueId());

        plugin.getMessageManager().send(sender, "admin-slot-given",
            "%player%", target.getName(),
            "%slot%", String.valueOf(slot));
        plugin.getMessageManager().send(target, "admin-slot-given-target",
            "%slot%", String.valueOf(slot));
    }

    /**
     * Give a player a backpack item.
     * Usage: /vaultpack giveitem <player> <type> [amount]
     */
    @Subcommand("giveitem")
    @CommandPermission("vaultpack.admin")
    @Description("Give a player a backpack item")
    @CommandCompletion("@players @nothing")
    @Syntax("<player> <type> [amount]")
    public void onGiveItem(CommandSender sender, Player target, String backpackTypeId, @Default("1") int amount) {
        com.vaultpack.types.BackpackType backpackType = plugin.getBackpackTypeManager().getBackpackType(backpackTypeId);

        if (backpackType == null) {
            plugin.getMessageManager().send(sender, "admin-invalid-type");
            for (String typeId : plugin.getBackpackTypeManager().getAllBackpackTypes().keySet()) {
                sender.sendMessage(ChatColor.GRAY + "  - " + typeId);
            }
            return;
        }

        if (amount < 1 || amount > 64) {
            plugin.getMessageManager().send(sender, "admin-invalid-amount");
            return;
        }

        // Create the backpack item
        ItemStack backpackItem = plugin.getBackpackTypeManager().createBackpackItem(backpackType);
        backpackItem.setAmount(amount);
        target.getInventory().addItem(backpackItem);

        plugin.getMessageManager().send(sender, "admin-backpack-given",
            "%player%", target.getName(),
            "%amount%", String.valueOf(amount),
            "%type%", ChatColor.translateAlternateColorCodes('&', backpackType.getDisplayName()));
        plugin.getMessageManager().send(target, "admin-backpack-given-target",
            "%amount%", String.valueOf(amount),
            "%type%", ChatColor.translateAlternateColorCodes('&', backpackType.getDisplayName()));
    }

    /**
     * List all available backpack types.
     * Usage: /vaultpack list
     */
    @Subcommand("list|types")
    @Description("List all available backpack types")
    public void onList(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY + "-----------------------------");
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Available Backpack Types");
        sender.sendMessage(ChatColor.DARK_GRAY + "-----------------------------");

        for (com.vaultpack.types.BackpackType type : plugin.getBackpackTypeManager().getAllBackpackTypes().values()) {
            sender.sendMessage(ChatColor.YELLOW + type.getId() + ChatColor.GRAY + " - " +
                    ChatColor.translateAlternateColorCodes('&', type.getDisplayName()));
            sender.sendMessage(ChatColor.GRAY + "  Tier: " + type.getDefaultTier().getDisplayName() +
                    " | Size: " + type.getDefaultTier().getSize() + " slots");
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "-----------------------------");
    }

    /**
     * Clear a player's backpack.
     * Usage: /vaultpack clear <player> <slot>
     */
    @Subcommand("clear")
    @CommandPermission("vaultpack.admin")
    @Description("Clear a player's backpack")
    @CommandCompletion("@players @backpackSlots")
    @Syntax("<player> <slot>")
    public void onClear(CommandSender sender, Player target, int slot) {
        PlayerBackpackData data = plugin.getDataManager().getPlayerData(target.getUniqueId());

        if (data.hasBackpack(slot)) {
            data.getBackpack(slot).getContents().clear();
            plugin.getDataManager().savePlayerData(target.getUniqueId());

            plugin.getMessageManager().send(sender, "admin-backpack-cleared",
                "%player%", target.getName(),
                "%slot%", String.valueOf(slot));
            plugin.getMessageManager().send(target, "admin-backpack-cleared-target",
                "%slot%", String.valueOf(slot));
        } else {
            plugin.getMessageManager().send(sender, "admin-backpack-no-backpack");
        }
    }

    /**
     * Reset a player's data (DANGEROUS).
     * Usage: /vaultpack reset <player>
     */
    @Subcommand("reset")
    @CommandPermission("vaultpack.admin")
    @Description("Reset a player's data (DANGEROUS)")
    @CommandCompletion("@players")
    @Syntax("<player>")
    public void onReset(CommandSender sender, String playerName) {
        // Support both online and offline players
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        UUID targetUUID = null;

        if (onlinePlayer != null) {
            targetUUID = onlinePlayer.getUniqueId();
        } else {
            // Try to find offline player
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            if (offlinePlayer.hasPlayedBefore()) {
                targetUUID = offlinePlayer.getUniqueId();
                playerName = offlinePlayer.getName();
            }
        }

        if (targetUUID == null) {
            plugin.getMessageManager().send(sender, "player-not-found");
            return;
        }

        // Close any open backpacks/ender pages
        if (onlinePlayer != null) {
            if (plugin.getBackpackManager().isBackpackOpen(onlinePlayer)) {
                onlinePlayer.closeInventory();
            }
            if (plugin.getEnderChestManager().isEnderPageOpen(onlinePlayer)) {
                onlinePlayer.closeInventory();
            }
        }

        // Reset player data
        plugin.getDataManager().resetPlayerData(targetUUID);

        List<String> resetMessages = plugin.getMessageManager().getMessageList("admin.reset-success");
        for (String message : resetMessages) {
            sender.sendMessage(message.replace("%player%", playerName));
        }

        if (onlinePlayer != null) {
            plugin.getMessageManager().send(onlinePlayer, "admin.reset-target");
        }
    }

    /**
     * Inspect a player's backpacks and ender chests.
     * Usage: /vaultpack inspect <player>
     */
    @Subcommand("inspect|view")
    @CommandPermission("vaultpack.admin")
    @Description("Inspect a player's backpacks and ender chests")
    @CommandCompletion("@players")
    @Syntax("<player>")
    public void onInspect(CommandSender sender, Player target) {
        PlayerBackpackData data = plugin.getDataManager().getPlayerData(target.getUniqueId());

        // Display inspect header
        List<String> headerMessages = plugin.getMessageManager().getMessageList("admin.inspect-header");
        for (String message : headerMessages) {
            sender.sendMessage(message.replace("%player%", target.getName()));
        }

        // Backpack information
        List<String> backpackMessages = plugin.getMessageManager().getMessageList("admin.inspect-backpacks");
        for (String message : backpackMessages) {
            String formatted = message
                .replace("%unlocked%", String.valueOf(data.getUnlockedSlots()))
                .replace("%max%", String.valueOf(plugin.getConfigManager().getMaxBackpackSlots()))
                .replace("%active%", String.valueOf(data.getActiveBackpackCount()))
                .replace("%total_slots%", String.valueOf(data.getTotalStorageSlots()))
                .replace("%used_slots%", String.valueOf(data.getTotalUsedSlots()));
            sender.sendMessage(formatted);
        }

        // List each backpack
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "Backpack Details:");
        for (int i = 1; i <= 18; i++) {
            if (data.hasBackpack(i)) {
                com.vaultpack.models.Backpack backpack = data.getBackpack(i);
                String tierColor = ChatColor.translateAlternateColorCodes('&', backpack.getTier().getColorCode());
                sender.sendMessage(ChatColor.GRAY + "  Slot #" + i + ": " + tierColor + backpack.getTier().getDisplayName() +
                        ChatColor.GRAY + " (" + backpack.getUsedSlots() + "/" + backpack.getSize() + " used)");
            }
        }

        // Ender chest information
        sender.sendMessage("");
        List<String> enderMessages = plugin.getMessageManager().getMessageList("admin.inspect-enderchest");
        for (String message : enderMessages) {
            String formatted = message
                .replace("%unlocked%", String.valueOf(data.getUnlockedEnderPages()))
                .replace("%total_slots%", String.valueOf(data.getTotalEnderStorageSlots()))
                .replace("%used_slots%", String.valueOf(data.getTotalUsedEnderSlots()));
            sender.sendMessage(formatted);
        }

        // List each ender page
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "Ender Page Details:");
        for (int i = 1; i <= 9; i++) {
            if (data.isEnderPageUnlocked(i)) {
                com.vaultpack.models.EnderPage page = data.getEnderPage(i);
                if (page != null) {
                    sender.sendMessage(ChatColor.GRAY + "  Page " + i + ": " +
                            ChatColor.WHITE + page.getUsedSlots() + "/45 used");
                } else {
                    sender.sendMessage(ChatColor.GRAY + "  Page " + i + ": " + ChatColor.YELLOW + "Empty");
                }
            }
        }

        // Display inspect footer
        List<String> footerMessages = plugin.getMessageManager().getMessageList("admin.inspect-footer");
        for (String message : footerMessages) {
            sender.sendMessage(message);
        }
    }

    /**
     * Send help message based on permissions.
     */
    private void sendHelp(CommandSender sender) {
        String helpKey = sender.hasPermission("vaultpack.admin") ? "admin.help-admin" : "admin.help-player";
        List<String> helpMessages = plugin.getMessageManager().getMessageList(helpKey);

        for (String message : helpMessages) {
            sender.sendMessage(message);
        }
    }
}

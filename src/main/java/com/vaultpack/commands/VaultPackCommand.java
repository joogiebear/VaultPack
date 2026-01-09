package com.vaultpack.commands;

import com.vaultpack.VaultPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main admin command for VaultPack
 * Usage: /vaultpack <reload|help|version|give|giveitem|list|clear>
 */
public class VaultPackCommand implements CommandExecutor, TabCompleter {

    private final VaultPackPlugin plugin;

    public VaultPackCommand(VaultPackPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // No args - show help
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "reload":
                return handleReload(sender);

            case "help":
                sendHelp(sender);
                return true;

            case "version":
            case "ver":
                return handleVersion(sender);

            case "give":
                return handleGive(sender, args);

            case "giveitem":
                return handleGiveItem(sender, args);

            case "list":
            case "types":
                return handleList(sender);

            case "clear":
                return handleClear(sender, args);

            case "reset":
                return handleReset(sender, args);

            case "inspect":
            case "view":
                return handleInspect(sender, args);

            default:
                plugin.getMessageManager().send(sender, "admin-unknown-command");
                return true;
        }
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("vaultpack.admin")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return true;
        }

        plugin.getMessageManager().send(sender, "admin-reload-start");
        plugin.reload();

        List<String> reloadMessages = plugin.getMessageManager().getMessageList("admin.reload-success");
        for (String message : reloadMessages) {
            sender.sendMessage(message.replace("%version%", plugin.getDescription().getVersion()));
        }
        return true;
    }

    private boolean handleVersion(CommandSender sender) {
        List<String> versionMessages = plugin.getMessageManager().getMessageList("admin.version");
        for (String message : versionMessages) {
            String formatted = message
                .replace("%version%", plugin.getDescription().getVersion())
                .replace("%vault%", plugin.isVaultEnabled() ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗")
                .replace("%papi%", plugin.isPlaceholderAPIEnabled() ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗");
            sender.sendMessage(formatted);
        }
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vaultpack.admin")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return true;
        }

        if (args.length < 3) {
            plugin.getMessageManager().send(sender, "admin.usage.give");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            plugin.getMessageManager().send(sender, "player-not-found");
            return true;
        }

        try {
            int slot = Integer.parseInt(args[2]);
            if (slot < 1 || slot > plugin.getConfigManager().getMaxBackpackSlots()) {
                plugin.getMessageManager().send(sender, "invalid-slot",
                    "%max%", String.valueOf(plugin.getConfigManager().getMaxBackpackSlots()));
                return true;
            }

            com.vaultpack.models.PlayerBackpackData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
            data.unlockSlot(slot);
            plugin.getDataManager().savePlayerData(target.getUniqueId());

            plugin.getMessageManager().send(sender, "admin-slot-given",
                "%player%", target.getName(),
                "%slot%", String.valueOf(slot));
            plugin.getMessageManager().send(target, "admin-slot-given-target",
                "%slot%", String.valueOf(slot));

        } catch (NumberFormatException e) {
            plugin.getMessageManager().send(sender, "invalid-slot", "%max%", "1-18");
        }

        return true;
    }

    private boolean handleGiveItem(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vaultpack.admin")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return true;
        }

        if (args.length < 3) {
            plugin.getMessageManager().send(sender, "admin.usage.giveitem");
            plugin.getMessageManager().send(sender, "admin.usage.giveitem-types",
                "%types%", String.join(", ", plugin.getBackpackTypeManager().getAllBackpackTypes().keySet()));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            plugin.getMessageManager().send(sender, "player-not-found");
            return true;
        }

        String backpackTypeId = args[2];
        com.vaultpack.types.BackpackType backpackType = plugin.getBackpackTypeManager().getBackpackType(backpackTypeId);

        if (backpackType == null) {
            plugin.getMessageManager().send(sender, "admin-invalid-type");
            for (String typeId : plugin.getBackpackTypeManager().getAllBackpackTypes().keySet()) {
                sender.sendMessage(ChatColor.GRAY + "  - " + typeId);
            }
            return true;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                plugin.getMessageManager().send(sender, "admin-invalid-amount");
                return true;
            }
        }

        // Create the backpack item using BackpackTypeManager
        org.bukkit.inventory.ItemStack backpackItem = plugin.getBackpackTypeManager().createBackpackItem(backpackType);
        backpackItem.setAmount(amount);
        target.getInventory().addItem(backpackItem);

        plugin.getMessageManager().send(sender, "admin-backpack-given",
            "%player%", target.getName(),
            "%amount%", String.valueOf(amount),
            "%type%", ChatColor.translateAlternateColorCodes('&', backpackType.getDisplayName()));
        plugin.getMessageManager().send(target, "admin-backpack-given-target",
            "%amount%", String.valueOf(amount),
            "%type%", ChatColor.translateAlternateColorCodes('&', backpackType.getDisplayName()));

        return true;
    }

    private boolean handleList(CommandSender sender) {
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
        return true;
    }

    private boolean handleClear(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vaultpack.admin")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return true;
        }

        if (args.length < 3) {
            plugin.getMessageManager().send(sender, "admin.usage.clear");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            plugin.getMessageManager().send(sender, "player-not-found");
            return true;
        }

        try {
            int slot = Integer.parseInt(args[2]);

            com.vaultpack.models.PlayerBackpackData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
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

        } catch (NumberFormatException e) {
            plugin.getMessageManager().send(sender, "invalid-slot", "%max%", "1-18");
        }

        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vaultpack.admin")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return true;
        }

        if (args.length < 2) {
            plugin.getMessageManager().send(sender, "admin.usage.reset");
            plugin.getMessageManager().send(sender, "admin.usage.reset-warning");
            return true;
        }

        // Support both online and offline players
        String playerName = args[1];
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        java.util.UUID targetUUID = null;

        if (onlinePlayer != null) {
            targetUUID = onlinePlayer.getUniqueId();
        } else {
            // Try to find offline player
            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            if (offlinePlayer.hasPlayedBefore()) {
                targetUUID = offlinePlayer.getUniqueId();
                playerName = offlinePlayer.getName();
            }
        }

        if (targetUUID == null) {
            plugin.getMessageManager().send(sender, "player-not-found");
            return true;
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

        return true;
    }

    private boolean handleInspect(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vaultpack.admin")) {
            plugin.getMessageManager().send(sender, "no-permission");
            return true;
        }

        if (args.length < 2) {
            plugin.getMessageManager().send(sender, "admin.usage.inspect");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            plugin.getMessageManager().send(sender, "admin-player-offline");
            return true;
        }

        com.vaultpack.models.PlayerBackpackData data = plugin.getDataManager().getPlayerData(target.getUniqueId());

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
        return true;
    }

    private void sendHelp(CommandSender sender) {
        // Show different help based on permission
        String helpKey = sender.hasPermission("vaultpack.admin") ? "admin.help-admin" : "admin.help-player";
        List<String> helpMessages = plugin.getMessageManager().getMessageList(helpKey);

        for (String message : helpMessages) {
            sender.sendMessage(message);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("help", "version"));

            if (sender.hasPermission("vaultpack.admin")) {
                completions.addAll(Arrays.asList("reload", "give", "giveitem", "list", "types", "clear", "reset", "inspect", "view"));
            }
        } else if (args.length == 2 && sender.hasPermission("vaultpack.admin")) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("giveitem") ||
                    args[0].equalsIgnoreCase("clear") || args[0].equalsIgnoreCase("reset") ||
                    args[0].equalsIgnoreCase("inspect") || args[0].equalsIgnoreCase("view")) {
                // Add online player names
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 3 && sender.hasPermission("vaultpack.admin")) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("clear")) {
                // Add slot numbers
                for (int i = 1; i <= plugin.getConfigManager().getMaxBackpackSlots(); i++) {
                    completions.add(String.valueOf(i));
                }
            } else if (args[0].equalsIgnoreCase("giveitem")) {
                // Add backpack types
                completions.addAll(plugin.getBackpackTypeManager().getAllBackpackTypes().keySet());
            }
        } else if (args.length == 4 && sender.hasPermission("vaultpack.admin")) {
            if (args[0].equalsIgnoreCase("giveitem")) {
                // Add amount suggestions
                completions.addAll(Arrays.asList("1", "5", "10", "64"));
            }
        }

        return completions;
    }
}

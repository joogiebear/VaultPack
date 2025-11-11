package com.vaultpack.commands;

import com.vaultpack.VaultPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

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
                sender.sendMessage(ChatColor.RED + "Unknown command! Use /vaultpack help");
                return true;
        }
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("vaultpack.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to reload!");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Reloading VaultPack...");
        plugin.reload();
        sender.sendMessage(ChatColor.GREEN + "VaultPack v" + plugin.getDescription().getVersion() + " reloaded successfully!");
        sender.sendMessage(ChatColor.GRAY + "  • Config reloaded");
        sender.sendMessage(ChatColor.GRAY + "  • Menus reloaded");
        sender.sendMessage(ChatColor.GRAY + "  • Backpack types reloaded");
        return true;
    }

    private boolean handleVersion(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY + "-----------------------------");
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "VaultPack");
        sender.sendMessage(ChatColor.GRAY + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.GRAY + "Author: " + ChatColor.WHITE + "VaultPack Team");
        sender.sendMessage(ChatColor.GRAY + "Vault: " + (plugin.isVaultEnabled() ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗"));
        sender.sendMessage(ChatColor.GRAY + "PlaceholderAPI: " + (plugin.isPlaceholderAPIEnabled() ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗"));
        sender.sendMessage(ChatColor.DARK_GRAY + "-----------------------------");
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vaultpack.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /vaultpack give <player> <slot>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        try {
            int slot = Integer.parseInt(args[2]);
            if (slot < 1 || slot > plugin.getConfigManager().getMaxBackpackSlots()) {
                sender.sendMessage(ChatColor.RED + "Invalid slot! Must be 1-" +
                        plugin.getConfigManager().getMaxBackpackSlots());
                return true;
            }

            com.vaultpack.models.PlayerBackpackData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
            data.unlockSlot(slot);
            plugin.getDataManager().savePlayerData(target.getUniqueId());

            sender.sendMessage(ChatColor.GREEN + "Gave " + target.getName() + " access to backpack slot #" + slot);
            target.sendMessage(ChatColor.GREEN + "You've been given access to backpack slot #" + slot + "!");

        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid slot number!");
        }

        return true;
    }

    private boolean handleGiveItem(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vaultpack.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /vaultpack giveitem <player> <backpack_type> [amount]");
            sender.sendMessage(ChatColor.GRAY + "Available types: " + String.join(", ", plugin.getBackpackTypeManager().getAllBackpackTypes().keySet()));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        String backpackTypeId = args[2];
        com.vaultpack.types.BackpackType backpackType = plugin.getBackpackTypeManager().getBackpackType(backpackTypeId);

        if (backpackType == null) {
            sender.sendMessage(ChatColor.RED + "Invalid backpack type! Available types:");
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
                sender.sendMessage(ChatColor.RED + "Invalid amount!");
                return true;
            }
        }

        // Create the backpack item using BackpackTypeManager
        org.bukkit.inventory.ItemStack backpackItem = plugin.getBackpackTypeManager().createBackpackItem(backpackType);
        backpackItem.setAmount(amount);
        target.getInventory().addItem(backpackItem);

        sender.sendMessage(ChatColor.GREEN + "Gave " + target.getName() + " " + amount + "x " +
                ChatColor.translateAlternateColorCodes('&', backpackType.getDisplayName()));
        target.sendMessage(ChatColor.GREEN + "You received " +
                ChatColor.translateAlternateColorCodes('&', backpackType.getDisplayName()) + "!");

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
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /vaultpack clear <player> <slot>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        try {
            int slot = Integer.parseInt(args[2]);

            com.vaultpack.models.PlayerBackpackData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
            if (data.hasBackpack(slot)) {
                data.getBackpack(slot).getContents().clear();
                plugin.getDataManager().savePlayerData(target.getUniqueId());

                sender.sendMessage(ChatColor.GREEN + "Cleared " + target.getName() + "'s backpack #" + slot);
                target.sendMessage(ChatColor.YELLOW + "Your backpack #" + slot + " was cleared by an admin!");
            } else {
                sender.sendMessage(ChatColor.RED + "Player doesn't have a backpack in that slot!");
            }

        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid slot number!");
        }

        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vaultpack.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /vaultpack reset <player>");
            sender.sendMessage(ChatColor.YELLOW + "Warning: This will delete ALL backpack and ender chest data!");
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
            sender.sendMessage(ChatColor.RED + "Player not found!");
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

        sender.sendMessage(ChatColor.GREEN + "Successfully reset data for " + playerName);
        sender.sendMessage(ChatColor.GRAY + "  • All backpacks removed");
        sender.sendMessage(ChatColor.GRAY + "  • All ender chest pages cleared");
        sender.sendMessage(ChatColor.GRAY + "  • Slot unlocks reset to default");

        if (onlinePlayer != null) {
            onlinePlayer.sendMessage(ChatColor.YELLOW + "Your VaultPack data has been reset by an admin!");
        }

        return true;
    }

    private boolean handleInspect(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vaultpack.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /vaultpack inspect <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found or not online!");
            return true;
        }

        com.vaultpack.models.PlayerBackpackData data = plugin.getDataManager().getPlayerData(target.getUniqueId());

        sender.sendMessage(ChatColor.DARK_GRAY + "==========================================");
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "VaultPack Data: " + ChatColor.YELLOW + target.getName());
        sender.sendMessage(ChatColor.DARK_GRAY + "==========================================");

        // Backpack information
        sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "Backpacks:");
        sender.sendMessage(ChatColor.GRAY + "  Unlocked Slots: " + ChatColor.WHITE + data.getUnlockedSlots() + "/" + plugin.getConfigManager().getMaxBackpackSlots());
        sender.sendMessage(ChatColor.GRAY + "  Active Backpacks: " + ChatColor.WHITE + data.getActiveBackpackCount());
        sender.sendMessage(ChatColor.GRAY + "  Total Storage: " + ChatColor.WHITE + data.getTotalStorageSlots() + " slots");
        sender.sendMessage(ChatColor.GRAY + "  Used Storage: " + ChatColor.WHITE + data.getTotalUsedSlots() + " slots");

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
        sender.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Ender Chest:");
        sender.sendMessage(ChatColor.GRAY + "  Unlocked Pages: " + ChatColor.WHITE + data.getUnlockedEnderPages() + "/9");
        sender.sendMessage(ChatColor.GRAY + "  Total Storage: " + ChatColor.WHITE + data.getTotalEnderStorageSlots() + " slots");
        sender.sendMessage(ChatColor.GRAY + "  Used Storage: " + ChatColor.WHITE + data.getTotalUsedEnderSlots() + " slots");

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

        sender.sendMessage(ChatColor.DARK_GRAY + "==========================================");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY + "-----------------------------");
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "VaultPack Admin Commands");
        sender.sendMessage(ChatColor.DARK_GRAY + "-----------------------------");
        sender.sendMessage(ChatColor.YELLOW + "/vaultpack help " + ChatColor.GRAY + "- Show this help");
        sender.sendMessage(ChatColor.YELLOW + "/vaultpack version " + ChatColor.GRAY + "- Show plugin info");

        if (sender.hasPermission("vaultpack.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/vaultpack reload " + ChatColor.GRAY + "- Reload configuration");
            sender.sendMessage(ChatColor.YELLOW + "/vaultpack give <player> <slot> " + ChatColor.GRAY + "- Give slot access");
            sender.sendMessage(ChatColor.YELLOW + "/vaultpack giveitem <player> <type> [amt] " + ChatColor.GRAY + "- Give backpack item");
            sender.sendMessage(ChatColor.YELLOW + "/vaultpack list " + ChatColor.GRAY + "- List backpack types");
            sender.sendMessage(ChatColor.YELLOW + "/vaultpack clear <player> <slot> " + ChatColor.GRAY + "- Clear backpack contents");
            sender.sendMessage(ChatColor.YELLOW + "/vaultpack reset <player> " + ChatColor.GRAY + "- Reset ALL player data");
            sender.sendMessage(ChatColor.YELLOW + "/vaultpack inspect <player> " + ChatColor.GRAY + "- View player's storage");
        }

        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Player commands:");
        sender.sendMessage(ChatColor.YELLOW + "/storage " + ChatColor.GRAY + "- Open unified storage GUI");
        sender.sendMessage(ChatColor.YELLOW + "/backpack " + ChatColor.GRAY + "- Open backpack selector");
        sender.sendMessage(ChatColor.YELLOW + "/backpack [slot] " + ChatColor.GRAY + "- Open specific backpack");
        sender.sendMessage(ChatColor.YELLOW + "/enderchest [page] " + ChatColor.GRAY + "- Open ender chest");
        sender.sendMessage(ChatColor.DARK_GRAY + "-----------------------------");
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

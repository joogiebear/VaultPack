package com.vaultpack.commands;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.gui.BackpackSelectorGUI;
import com.vaultpack.models.PlayerBackpackData;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BackpackCommand implements CommandExecutor, TabCompleter {

    private final VaultPackPlugin plugin;
    private final BackpackSelectorGUI backpackGUI;

    public BackpackCommand(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.backpackGUI = new BackpackSelectorGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /backpack - open menu
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("vaultpack.use")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use backpacks!");
                return true;
            }

            // Open backpack selector GUI (v1.0.0 - uses menus/backpack_selector.yml)
            backpackGUI.open(player);
            return true;
        }

        // v2.0.0: /backpack [slot] - open specific backpack slot
        // Check if first arg is a number (slot number)
        try {
            int slotNumber = Integer.parseInt(args[0]);

            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("vaultpack.use")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use backpacks!");
                return true;
            }

            // Validate slot number range
            if (slotNumber < 1 || slotNumber > 18) {
                player.sendMessage(ChatColor.RED + "Backpack slot must be between 1 and 18!");
                return true;
            }

            // Check if slot is unlocked
            PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
            if (!data.isSlotUnlocked(slotNumber)) {
                player.sendMessage(ChatColor.RED + "You haven't unlocked backpack slot " + slotNumber + " yet!");
                player.sendMessage(ChatColor.YELLOW + "Open /storage to unlock it.");
                return true;
            }

            // Check if backpack exists in that slot
            if (!data.hasBackpack(slotNumber)) {
                player.sendMessage(ChatColor.RED + "You don't have a backpack in slot " + slotNumber + "!");
                player.sendMessage(ChatColor.YELLOW + "Place a backpack item in that slot via /storage.");
                return true;
            }

            // Open the backpack
            plugin.getBackpackManager().openBackpack(player, slotNumber);
            return true;

        } catch (NumberFormatException ignored) {
            // Not a number, continue to check other subcommands
        }

        // /backpack help
        if (args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Unknown command! Use /backpack help");
        sender.sendMessage(ChatColor.GRAY + "For admin commands, use /vaultpack");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY + "-----------------------------");
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Backpack Commands");
        sender.sendMessage(ChatColor.DARK_GRAY + "-----------------------------");
        sender.sendMessage(ChatColor.YELLOW + "/backpack " + ChatColor.GRAY + "- Open backpack selector GUI");
        sender.sendMessage(ChatColor.YELLOW + "/backpack [slot] " + ChatColor.GRAY + "- Open specific backpack (1-18)");
        sender.sendMessage(ChatColor.YELLOW + "/backpack help " + ChatColor.GRAY + "- Show this help");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Other player commands:");
        sender.sendMessage(ChatColor.YELLOW + "/storage " + ChatColor.GRAY + "- Open unified storage GUI");
        sender.sendMessage(ChatColor.YELLOW + "/enderchest [page] " + ChatColor.GRAY + "- Open ender chest (1-9)");

        if (sender.hasPermission("vaultpack.admin")) {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Admin Commands");
            sender.sendMessage(ChatColor.YELLOW + "/vaultpack help " + ChatColor.GRAY + "- View all admin commands");
        }

        sender.sendMessage(ChatColor.DARK_GRAY + "-----------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Add slot numbers for /backpack [slot]
            if (sender instanceof Player) {
                Player player = (Player) sender;
                PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

                // Suggest unlocked backpack slots that have backpacks
                for (int i = 1; i <= 18; i++) {
                    if (data.isSlotUnlocked(i) && data.hasBackpack(i)) {
                        completions.add(String.valueOf(i));
                    }
                }
            }

            completions.add("help");
        }

        return completions;
    }
}

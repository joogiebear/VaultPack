package com.vaultpack.commands;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.gui.BackpackSelectorGUI;
import com.vaultpack.models.PlayerBackpackData;
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
                plugin.getMessageManager().send(sender, "command-player-only");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("vaultpack.use")) {
                plugin.getMessageManager().send(player, "no-permission");
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
                plugin.getMessageManager().send(sender, "command-player-only");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("vaultpack.use")) {
                plugin.getMessageManager().send(player, "no-permission");
                return true;
            }

            // Validate slot number range
            if (slotNumber < 1 || slotNumber > 18) {
                plugin.getMessageManager().send(player, "invalid-slot", "%max%", "18");
                return true;
            }

            // Check if slot is unlocked
            PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
            if (!data.isSlotUnlocked(slotNumber)) {
                plugin.getMessageManager().send(player, "slot-locked");
                return true;
            }

            // Check if backpack exists in that slot
            if (!data.hasBackpack(slotNumber)) {
                plugin.getMessageManager().send(player, "backpack-remove-fail");
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

        // Unknown command
        sender.sendMessage(plugin.getMessageManager().getMessage("no-permission")); // Temporary - will use proper error message
        return true;
    }

    private void sendHelp(CommandSender sender) {
        // Show appropriate help based on permission
        String helpKey = "commands.help";
        List<String> helpMessages = plugin.getMessageManager().getMessageList(helpKey);

        for (String message : helpMessages) {
            sender.sendMessage(message);
        }

        // Show admin commands only to admins
        if (sender.hasPermission("vaultpack.admin")) {
            List<String> adminHint = plugin.getMessageManager().getMessageList("admin.help-admin-hint");
            for (String message : adminHint) {
                sender.sendMessage(message);
            }
        }
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

package com.vaultpack.commands;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.gui.EnderChestGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * v1.0.0: /enderchest command
 * Opens ender chest page selector or specific page
 */
public class EnderChestCommand implements CommandExecutor, TabCompleter {

    private final VaultPackPlugin plugin;
    private final EnderChestGUI enderChestGUI;

    public EnderChestCommand(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.enderChestGUI = new EnderChestGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageManager().send(sender, "command-player-only");
            return true;
        }

        Player player = (Player) sender;

        // Check permission
        if (!player.hasPermission("vaultpack.use")) {
            plugin.getMessageManager().send(player, "no-permission");
            return true;
        }

        // No args - open the page selector GUI (v1.0.0)
        if (args.length == 0) {
            enderChestGUI.open(player);
            return true;
        }

        // With page number - open that specific page directly
        int pageNumber;
        try {
            pageNumber = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            plugin.getMessageManager().send(player, "enderchest-invalid-page");
            return true;
        }

        // Validate page number range
        if (pageNumber < 1 || pageNumber > 9) {
            plugin.getMessageManager().send(player, "enderchest-invalid-page");
            return true;
        }

        // Check if page is unlocked
        if (!plugin.getDataManager().getPlayerData(player.getUniqueId()).isEnderPageUnlocked(pageNumber)) {
            plugin.getMessageManager().send(player, "enderchest-page-locked", "%page%", String.valueOf(pageNumber));
            plugin.getMessageManager().send(player, "enderchest-view-pages");
            return true;
        }

        // Open the ender page directly
        plugin.getEnderChestManager().openEnderPage(player, pageNumber);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                int unlockedPages = plugin.getDataManager().getPlayerData(player.getUniqueId()).getUnlockedEnderPages();

                // Suggest unlocked page numbers
                for (int i = 1; i <= unlockedPages; i++) {
                    completions.add(String.valueOf(i));
                }
            }
        }

        return completions;
    }
}

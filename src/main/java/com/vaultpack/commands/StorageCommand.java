package com.vaultpack.commands;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.gui.StorageMenuGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * v1.0.0: /storage command
 * Opens the unified storage GUI (menus/storage.yml)
 */
public class StorageCommand implements CommandExecutor, TabCompleter {

    private final VaultPackPlugin plugin;
    private final StorageMenuGUI storageGUI;

    public StorageCommand(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.storageGUI = new StorageMenuGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        // Check permission
        if (!player.hasPermission("vaultpack.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        // Open the unified storage GUI
        storageGUI.open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}

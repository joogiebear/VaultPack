package com.vaultpack.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.vaultpack.VaultPackPlugin;
import com.vaultpack.gui.StorageMenuGUI;
import org.bukkit.entity.Player;

/**
 * ACF-based storage command.
 * Opens the unified storage GUI (menus/storage.yml).
 */
@CommandAlias("storage|vault")
@Description("Open the unified storage menu")
public class StorageCommand extends BaseCommand {

    private final VaultPackPlugin plugin;
    private final StorageMenuGUI storageGUI;

    public StorageCommand(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.storageGUI = new StorageMenuGUI(plugin);
    }

    /**
     * Default command - opens unified storage GUI.
     * Usage: /storage
     */
    @Default
    @CommandPermission("vaultpack.use")
    @Description("Open your unified storage menu")
    public void onDefault(Player player) {
        storageGUI.open(player);
    }

    /**
     * Show storage command help.
     * Usage: /storage help
     */
    @Subcommand("help")
    @Description("Show storage command help")
    @HelpCommand
    public void onHelp(Player player) {
        player.sendMessage("§8§m                                               ");
        player.sendMessage("§6§lVaultPack Storage");
        player.sendMessage("");
        player.sendMessage("§e/storage §7- Open unified storage menu");
        player.sendMessage("§e/storage help §7- Show this help message");
        player.sendMessage("");
        player.sendMessage("§7Access all your backpacks and ender chests in one place!");
        player.sendMessage("§8§m                                               ");
    }
}

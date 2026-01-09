package com.vaultpack.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.vaultpack.VaultPackPlugin;
import com.vaultpack.gui.BackpackSelectorGUI;
import com.vaultpack.data.holders.PlayerDataHolder;
import org.bukkit.entity.Player;

/**
 * ACF-based backpack command.
 * Handles all backpack-related player commands using modern annotation-based approach.
 */
@CommandAlias("backpack|bp")
@Description("Manage your backpacks")
public class BackpackCommand extends BaseCommand {

    private final VaultPackPlugin plugin;
    private final BackpackSelectorGUI backpackGUI;

    public BackpackCommand(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.backpackGUI = new BackpackSelectorGUI(plugin);
    }

    /**
     * Default command - opens backpack selector GUI.
     * Usage: /backpack
     */
    @Default
    @CommandPermission("vaultpack.use")
    @Description("Open your backpack selector")
    public void onDefault(Player player) {
        backpackGUI.open(player);
    }

    /**
     * Open a specific backpack slot.
     * Usage: /backpack <slot>
     */
    @Subcommand("open")
    @CommandAlias("backpack")
    @CommandPermission("vaultpack.use")
    @Description("Open a specific backpack slot")
    @CommandCompletion("@backpackSlots")
    @Syntax("<slot> - Slot number (1-18)")
    public void onOpenSlot(Player player, int slotNumber) {
        // Validate slot number range
        if (slotNumber < 1 || slotNumber > plugin.getConfigManager().getMaxBackpackSlots()) {
            plugin.getMessageManager().send(player, "invalid-slot",
                "%max%", String.valueOf(plugin.getConfigManager().getMaxBackpackSlots()));
            return;
        }

        // Check if slot is unlocked
        PlayerDataHolder data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (!data.isSlotUnlocked(slotNumber)) {
            plugin.getMessageManager().send(player, "slot-locked");
            return;
        }

        // Check if backpack exists in that slot
        if (!data.hasBackpack(slotNumber)) {
            plugin.getMessageManager().send(player, "backpack-remove-fail");
            return;
        }

        // Open the backpack
        plugin.getBackpackManager().openBackpack(player, slotNumber);
    }

    /**
     * Show backpack command help.
     * Usage: /backpack help
     */
    @Subcommand("help")
    @Description("Show backpack command help")
    @HelpCommand
    public void onHelp(Player player) {
        player.sendMessage("§8§m                                               ");
        player.sendMessage("§6§lVaultPack Backpacks");
        player.sendMessage("");
        player.sendMessage("§e/backpack §7- Open backpack selector");
        player.sendMessage("§e/backpack <slot> §7- Open specific backpack slot");
        player.sendMessage("§e/backpack help §7- Show this help message");
        player.sendMessage("");
        player.sendMessage("§7Use the backpack selector GUI to manage your backpacks!");
        player.sendMessage("§8§m                                               ");
    }
}

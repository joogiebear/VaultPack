package com.vaultpack.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Called when a player closes a backpack
 * This event is NOT cancellable
 */
public class BackpackCloseEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final int slotNumber;

    public BackpackCloseEvent(Player player, int slotNumber) {
        super(player);
        this.slotNumber = slotNumber;
    }

    /**
     * Get the slot number of the backpack being closed
     * @return The slot number (1-18)
     */
    public int getSlotNumber() {
        return slotNumber;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

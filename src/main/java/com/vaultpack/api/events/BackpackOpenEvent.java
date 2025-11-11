package com.vaultpack.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Called when a player opens a backpack
 * This event is cancellable - if cancelled, the backpack will not open
 */
public class BackpackOpenEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    private final int slotNumber;

    public BackpackOpenEvent(Player player, int slotNumber) {
        super(player);
        this.slotNumber = slotNumber;
    }

    /**
     * Get the slot number of the backpack being opened
     * @return The slot number (1-18)
     */
    public int getSlotNumber() {
        return slotNumber;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

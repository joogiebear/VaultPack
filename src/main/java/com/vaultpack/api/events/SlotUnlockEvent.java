package com.vaultpack.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Called when a player unlocks a backpack slot
 * This event is cancellable
 */
public class SlotUnlockEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    private final int slotNumber;
    private final UnlockMethod method;

    public enum UnlockMethod {
        PERMISSION,
        ECONOMY,
        ADMIN_COMMAND,
        API
    }

    public SlotUnlockEvent(Player player, int slotNumber, UnlockMethod method) {
        super(player);
        this.slotNumber = slotNumber;
        this.method = method;
    }

    /**
     * Get the slot number being unlocked
     * @return The slot number (1-18)
     */
    public int getSlotNumber() {
        return slotNumber;
    }

    /**
     * Get the method used to unlock the slot
     * @return The UnlockMethod
     */
    public UnlockMethod getMethod() {
        return method;
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

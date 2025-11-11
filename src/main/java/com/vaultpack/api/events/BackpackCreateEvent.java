package com.vaultpack.api.events;

import com.vaultpack.models.BackpackTier;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Called when a backpack is created/placed in a slot
 * This event is cancellable
 */
public class BackpackCreateEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    private final int slotNumber;
    private final BackpackTier tier;

    public BackpackCreateEvent(Player player, int slotNumber, BackpackTier tier) {
        super(player);
        this.slotNumber = slotNumber;
        this.tier = tier;
    }

    /**
     * Get the slot number where the backpack is being placed
     * @return The slot number (1-18)
     */
    public int getSlotNumber() {
        return slotNumber;
    }

    /**
     * Get the tier of the backpack being created
     * @return The BackpackTier
     */
    public BackpackTier getTier() {
        return tier;
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

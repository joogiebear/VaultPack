package com.vaultpack.api.events;

import com.vaultpack.models.Backpack;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player opens a backpack.
 * This event is cancellable.
 *
 * @since 3.0.0
 */
@Getter
public class BackpackOpenEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int slot;
    private final Backpack backpack;

    @Setter
    private boolean cancelled = false;

    public BackpackOpenEvent(@NotNull Player player, int slot, @NotNull Backpack backpack) {
        super(player);
        this.slot = slot;
        this.backpack = backpack;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

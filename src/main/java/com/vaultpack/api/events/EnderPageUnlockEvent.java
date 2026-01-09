package com.vaultpack.api.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player unlocks an ender chest page.
 * This event is cancellable.
 *
 * @since 3.0.0
 */
@Getter
public class EnderPageUnlockEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int page;
    private final int cost;

    @Setter
    private boolean cancelled = false;

    public EnderPageUnlockEvent(@NotNull Player player, int page, int cost) {
        super(player);
        this.page = page;
        this.cost = cost;
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

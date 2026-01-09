package com.vaultpack.api.events;

import com.vaultpack.models.EnderPage;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player opens an ender chest page.
 * This event is cancellable.
 *
 * @since 3.0.0
 */
@Getter
public class EnderPageOpenEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int page;
    private final EnderPage enderPage;

    @Setter
    private boolean cancelled = false;

    public EnderPageOpenEvent(@NotNull Player player, int page, @NotNull EnderPage enderPage) {
        super(player);
        this.page = page;
        this.enderPage = enderPage;
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

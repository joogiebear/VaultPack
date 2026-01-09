package gg.auroramc.collections.api.event;

import gg.auroramc.collections.collection.Collection;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class CollectionLevelUpEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    private final Player player;
    private final Collection collection;
    private final long level;

    public CollectionLevelUpEvent(Player player, Collection collection, long level) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.collection = collection;
        this.level = level;
    }
}

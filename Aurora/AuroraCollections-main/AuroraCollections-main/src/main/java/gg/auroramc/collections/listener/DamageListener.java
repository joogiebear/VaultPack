package gg.auroramc.collections.listener;

import gg.auroramc.aurora.api.AuroraAPI;
import gg.auroramc.collections.AuroraCollections;
import gg.auroramc.collections.collection.Trigger;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DamageListener implements Listener {
    private final AuroraCollections plugin;

    public DamageListener(AuroraCollections plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        var damager = getDamager(event);
        var victim = event.getEntity();

        // Ignore if damager is not a player
        if (damager == null) return;
        // Ignore if victim is a player
        if (victim instanceof Player) return;

        var damage = event.getFinalDamage();

        // cap the damage to the victim's max health
        if (victim instanceof LivingEntity livingEntity) {
            if (damage > livingEntity.getHealth()) {
                damage = livingEntity.getHealth();
            }
        }

        var id = AuroraAPI.getEntityManager().resolveId(victim);

        plugin.getCollectionManager().progressCollections(damager, id, (int) Math.floor(damage), Trigger.ENTITY_DAMAGE);
    }

    private Player getDamager(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player player) {
                return player;
            }
        }
        return null;
    }
}

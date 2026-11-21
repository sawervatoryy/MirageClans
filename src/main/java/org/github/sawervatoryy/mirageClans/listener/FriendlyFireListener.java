package org.github.sawervatoryy.mirageClans.listener;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.github.sawervatoryy.mirageClans.MirageClans;
import org.github.sawervatoryy.mirageClans.model.Clan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Дружеский огонь. Владелец снаряда определяется для стрел, трезубцев, зелий (в т.ч. туманных облаков),
 * динамита и приручённых питомцев — соклановцы не могут обойти защиту.
 */
public final class FriendlyFireListener implements Listener {

    private final MirageClans plugin;

    public FriendlyFireListener(MirageClans plugin) {
        this.plugin = plugin;
    }

    /** Ищет игрока-виновника урона. */
    private Player resolve(Entity damager) {
        if (damager instanceof Player p) {
            return p;
        }
        if (damager instanceof Projectile pr && pr.getShooter() instanceof Player p) {
            return p;
        }
        if (damager instanceof AreaEffectCloud cloud) {
            ProjectileSource src = cloud.getSource();
            if (src instanceof Player p) {
                return p;
            }
        }
        if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player p) {
            return p;
        }
        if (damager instanceof Tameable t && t.getOwner() instanceof Player p) {
            return p;
        }
        return null;
    }

    /** true, если урон между соклановцами запрещён. */
    private boolean blocked(Player attacker, Player victim) {
        if (attacker.getUniqueId().equals(victim.getUniqueId())) {
            return false;
        }
        Clan a = plugin.clans().byPlayer(attacker.getUniqueId());
        if (a == null) {
            return false;
        }
        Clan v = plugin.clans().byPlayer(victim.getUniqueId());
        return a == v && !a.friendlyFire();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolve(e.getDamager());
        if (attacker != null && blocked(attacker, victim)) {
            e.setCancelled(true);
            plugin.lang().send(attacker, "friendly-fire.blocked");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombust(EntityCombustByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolve(e.getCombuster());
        if (attacker != null && blocked(attacker, victim)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSplash(PotionSplashEvent e) {
        if (!(e.getEntity().getShooter() instanceof Player thrower)) {
            return;
        }
        if (!harmful(e.getPotion().getEffects())) {
            return;
        }
        for (LivingEntity le : new ArrayList<>(e.getAffectedEntities())) {
            if (le instanceof Player victim && blocked(thrower, victim)) {
                e.setIntensity(le, 0);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCloud(AreaEffectCloudApplyEvent e) {
        AreaEffectCloud cloud = e.getEntity();
        if (!(cloud.getSource() instanceof Player thrower)) {
            return;
        }
        List<PotionEffect> effects = new ArrayList<>(cloud.getCustomEffects());
        if (cloud.getBasePotionType() != null) {
            effects.addAll(cloud.getBasePotionType().getPotionEffects());
        }
        if (!harmful(effects)) {
            return;
        }
        e.getAffectedEntities().removeIf(le -> le instanceof Player victim && blocked(thrower, victim));
    }

    private boolean harmful(Collection<PotionEffect> effects) {
        List<String> names = plugin.settings().cfg().getStringList("friendly-fire.harmful-effects");
        for (PotionEffect pe : effects) {
            for (String n : names) {
                PotionEffectType t = PotionEffectType.getByKey(NamespacedKey.minecraft(n.toLowerCase()));
                if (t != null && t.equals(pe.getType())) {
                    return true;
                }
            }
        }
        return false;
    }
}

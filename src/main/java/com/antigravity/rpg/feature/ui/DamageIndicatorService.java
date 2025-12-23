package com.antigravity.rpg.feature.ui;

import com.antigravity.rpg.api.service.Service;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 데미지 숫자를 홀로그램으로 표시하는 서비스입니다.
 */
@Singleton
public class DamageIndicatorService implements Service {

    private final JavaPlugin plugin;

    @Inject
    public DamageIndicatorService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public String getName() {
        return "DamageIndicatorService";
    }

    /**
     * 특정 위치에 데미지 숫자를 공중에 띄웁니다.
     */
    public void displayDamage(Location loc, double damage, boolean isCritical) {
        loc.add(Math.random() - 0.5, 1.5 + (Math.random() * 0.5), Math.random() - 0.5);

        ArmorStand indicator = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        indicator.setVisible(false);
        indicator.setGravity(false);
        indicator.setCustomNameVisible(true);
        indicator.setMarker(true);
        indicator.setBasePlate(false);
        indicator.setSmall(true);

        String text = isCritical ? "§e§l" + (int) damage + " §6§lCRIT!" : "§f" + (int) damage;
        indicator.setCustomName(text);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ > 20) {
                    indicator.remove();
                    cancel();
                    return;
                }
                indicator.teleport(indicator.getLocation().add(0, 0.05, 0));
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}

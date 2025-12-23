package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Fireball;

/**
 * 투사체를 발사하는 메카닉 구현체입니다.
 */
public class ProjectileMechanic implements Mechanic {

    @Override
    public void cast(SkillMetadata meta) {
        Player shooter = org.bukkit.Bukkit.getPlayer(meta.getCaster().getUuid());
        if (shooter == null)
            return;

        String type = (String) meta.getConfig().getOrDefault("projectile-type", "ARROW");
        float speed = ((Number) meta.getConfig().getOrDefault("speed", 1.5f)).floatValue();

        Class<? extends Projectile> projectileClass = Arrow.class;
        if (type.equalsIgnoreCase("FIREBALL")) {
            projectileClass = Fireball.class;
        }

        shooter.launchProjectile(projectileClass, shooter.getLocation().getDirection().multiply(speed));
    }
}

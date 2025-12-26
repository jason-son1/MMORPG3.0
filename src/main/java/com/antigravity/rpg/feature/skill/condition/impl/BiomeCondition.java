package com.antigravity.rpg.feature.skill.condition.impl;

import com.antigravity.rpg.api.skill.Condition;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import org.bukkit.block.Biome;
import org.bukkit.entity.Entity;

import java.util.Map;

/**
 * 특정 바이옴에 있는지 검사합니다.
 */
public class BiomeCondition implements Condition {

    private String biomeName;

    @Override
    public void setup(Map<String, Object> config) {
        this.biomeName = (String) config.get("biome");
    }

    @Override
    public boolean evaluate(SkillCastContext ctx, Entity target) {
        if (biomeName == null)
            return true;

        Entity entityToCheck = target != null ? target : (Entity) ctx.getCasterEntity();

        Biome bio;
        try {
            bio = Biome.valueOf(biomeName.toUpperCase());
        } catch (Exception e) {
            return false;
        }

        return entityToCheck.getLocation().getBlock().getBiome() == bio;
    }
}

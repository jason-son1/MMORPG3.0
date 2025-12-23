package com.antigravity.rpg.feature.skill.condition.impl;

import com.antigravity.rpg.feature.skill.condition.Condition;
import com.antigravity.rpg.feature.skill.context.SkillMetadata;
import org.bukkit.block.Biome;

import java.util.Map;

/**
 * 특정 바이옴에 있는지 검사합니다.
 */
public class BiomeCondition implements Condition {

    @Override
    public boolean evaluate(SkillMetadata meta, Map<String, Object> config) {
        String biomeName = (String) config.get("biome");
        if (biomeName == null)
            return true;

        Biome bio;
        try {
            bio = Biome.valueOf(biomeName.toUpperCase());
        } catch (Exception e) {
            return false;
        }

        return meta.getSourceEntity().getLocation().getBlock().getBiome() == bio;
    }
}

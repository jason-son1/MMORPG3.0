package com.antigravity.rpg.feature.skill.condition.impl;

import com.antigravity.rpg.feature.skill.condition.Condition;
import com.antigravity.rpg.feature.skill.context.SkillMetadata;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 특정 아이템을 들고 있는지 검사합니다.
 */
public class HoldingItemCondition implements Condition {

    @Override
    public boolean evaluate(SkillMetadata meta, Map<String, Object> config) {
        if (!(meta.getSourceEntity() instanceof Player))
            return false;
        Player player = (Player) meta.getSourceEntity();

        String materialName = (String) config.get("material");
        if (materialName == null)
            return true;

        Material mat;
        try {
            mat = Material.valueOf(materialName.toUpperCase());
        } catch (Exception e) {
            return false;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        return item.getType() == mat;
    }
}

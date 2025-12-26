package com.antigravity.rpg.feature.skill.condition.impl;

import com.antigravity.rpg.api.skill.Condition;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

/**
 * 시전자가 특정 아이템을 손에 들고 있는지 또는 특정 속성을 가진 아이템을 보유하고 있는지 확인하는 조건부입니다.
 */
public class ItemCondition implements Condition {

    private Material material;
    private String nameContains;
    private List<String> loreContains;

    @Override
    public void setup(Map<String, Object> config) {
        if (config.containsKey("material")) {
            this.material = Material.valueOf(((String) config.get("material")).toUpperCase());
        }
        this.nameContains = (String) config.get("name-contains");
        this.loreContains = (List<String>) config.get("lore-contains");
    }

    @Override
    public boolean evaluate(SkillCastContext ctx, Entity target) {
        if (!(ctx.getCasterEntity() instanceof Player))
            return false;

        Player player = (Player) ctx.getCasterEntity();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) {
            return material == null; // 아무것도 안 들고 있는 것이 조건일 때
        }

        if (material != null && item.getType() != material) {
            return false;
        }

        if (nameContains != null || loreContains != null) {
            if (!item.hasItemMeta())
                return false;
            ItemMeta meta = item.getItemMeta();

            if (nameContains != null && !meta.getDisplayName().contains(nameContains)) {
                return false;
            }

            if (loreContains != null) {
                List<String> lore = meta.getLore();
                if (lore == null)
                    return false;
                for (String line : loreContains) {
                    boolean found = false;
                    for (String itemLine : lore) {
                        if (itemLine.contains(line)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                        return false;
                }
            }
        }

        return true;
    }
}

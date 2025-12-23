package com.antigravity.rpg.feature.skill.condition.impl;

import com.antigravity.rpg.feature.skill.condition.Condition;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.entity.Entity;

import java.util.Map;
import java.util.Optional;

/**
 * 대상이 특정 MythicMob인지 또는 특정 팩션에 속해있는지 확인하는 조건부입니다.
 */
public class MythicMobCondition implements Condition {

    private String mobId;
    private String faction;

    @Override
    public void setup(Map<String, Object> config) {
        this.mobId = (String) config.get("mob-id");
        this.faction = (String) config.get("faction");
    }

    @Override
    public boolean evaluate(SkillCastContext ctx, Entity target) {
        if (target == null)
            return false;

        Optional<ActiveMob> am = MythicBukkit.inst().getMobManager().getActiveMob(target.getUniqueId());
        if (!am.isPresent())
            return false;

        ActiveMob activeMob = am.get();

        if (mobId != null && !activeMob.getType().getInternalName().equalsIgnoreCase(mobId)) {
            return false;
        }

        if (faction != null && !activeMob.getFaction().equalsIgnoreCase(faction)) {
            return false;
        }

        return true;
    }
}

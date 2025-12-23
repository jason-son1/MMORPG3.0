package com.antigravity.rpg.feature.skill.target.impl;

import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.feature.skill.target.Targeter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Collections;
import java.util.List;

/**
 * 시전자 자신을 대상으로 지정하는 타겟터입니다.
 */
public class SelfTargeter implements Targeter {

    @Override
    public List<Entity> getTargetEntities(SkillCastContext ctx) {
        return Collections.singletonList(ctx.getCasterEntity());
    }

    @Override
    public List<Location> getTargetLocations(SkillCastContext ctx) {
        return Collections.singletonList(ctx.getCasterEntity().getLocation());
    }
}

package com.antigravity.rpg.feature.skill.target.impl;

import com.antigravity.rpg.feature.skill.context.SkillMetadata;
import com.antigravity.rpg.feature.skill.target.Targeter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Collections;
import java.util.List;

/**
 * 시전자 자신을 타겟으로 지정하는 타겟터입니다.
 */
public class SelfTargeter implements Targeter {

    @Override
    public List<Entity> getTargetEntities(SkillMetadata meta) {
        if (meta.getSourceEntity() == null)
            return Collections.emptyList();
        return Collections.singletonList(meta.getSourceEntity());
    }

    @Override
    public List<Location> getTargetLocations(SkillMetadata meta) {
        if (meta.getSourceEntity() == null)
            return Collections.emptyList();
        return Collections.singletonList(meta.getSourceEntity().getLocation());
    }
}

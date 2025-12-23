package com.antigravity.rpg.feature.skill.target;

import com.antigravity.rpg.feature.skill.context.SkillMetadata;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.List;

/**
 * 스킬의 대상을 결정하는 인터페이스입니다.
 * 개별 엔티티 리스트 또는 위치 리스트를 반환할 수 있습니다.
 */
public interface Targeter {

    /**
     * SkillMetadata를 기반으로 타겟 엔티티 목록을 반환합니다.
     */
    List<Entity> getTargetEntities(SkillMetadata meta);

    /**
     * SkillMetadata를 기반으로 타겟 위치 목록을 반환합니다.
     */
    List<Location> getTargetLocations(SkillMetadata meta);
}

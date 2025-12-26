package com.antigravity.rpg.api.skill;

import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.List;

/**
 * 스킬의 효과 대상을 탐색하는 인터페이스입니다.
 */
public interface Targeter {

    /**
     * 조건에 맞는 엔티티 목록을 가져옵니다.
     */
    List<Entity> getTargetEntities(SkillCastContext ctx);

    /**
     * 조건에 맞는 위치 목록을 가져옵니다.
     */
    List<Location> getTargetLocations(SkillCastContext ctx);

    /**
     * 설정을 초기화합니다.
     */
    default void setup(java.util.Map<String, Object> config) {
    }
}

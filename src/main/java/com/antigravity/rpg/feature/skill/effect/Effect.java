package com.antigravity.rpg.feature.skill.effect;

import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Map;

/**
 * 스킬의 시각적/청각적 효과를 정의하는 인터페이스입니다.
 * 파티클, 사운드, 애니메이션 등이 이 인터페이스를 구현합니다.
 */
public interface Effect {

    /**
     * 효과 설정을 초기화합니다.
     * 
     * @param config YAML 설정 맵
     */
    void setup(Map<String, Object> config);

    /**
     * 효과를 실행합니다.
     * 
     * @param origin 실행 위치 (null일 수 있음)
     * @param target 대상 엔티티 (null일 수 있음)
     * @param ctx    스킬 시전 컨텍스트
     */
    void play(Location origin, Entity target, SkillCastContext ctx);
}

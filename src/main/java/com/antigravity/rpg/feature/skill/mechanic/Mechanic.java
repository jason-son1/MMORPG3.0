package com.antigravity.rpg.feature.skill.mechanic;

import com.antigravity.rpg.core.engine.trigger.TriggerContext;
import com.antigravity.rpg.feature.player.PlayerData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Entity;

import java.util.Map;

/**
 * 스킬의 개별 효과(메카닉)를 정의하는 인터페이스입니다.
 */
public interface Mechanic {

    /**
     * 메카닉을 실행합니다.
     * 
     * @param meta 스킬 시전 시의 메타데이터 (시전자, 대상, 설정값 등)
     */
    void cast(SkillMetadata meta);

    /**
     * 스킬 실행에 필요한 정보를 담는 데이터 클래스입니다.
     */
    @Getter
    @RequiredArgsConstructor
    class SkillMetadata {
        private final PlayerData caster;
        private final Entity target;
        private final TriggerContext context;
        private final Map<String, Object> config;
    }
}

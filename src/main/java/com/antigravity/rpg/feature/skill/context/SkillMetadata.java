package com.antigravity.rpg.feature.skill.context;

import com.antigravity.rpg.feature.player.PlayerData;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 스킬 실행 중 유지되는 모든 컨텍스트 정보를 담는 클래스입니다.
 * 시전자, 대상, 위치, 변수 등을 포함하며 모든 메카닉이 이를 공유합니다.
 */
@Getter
@Setter
@Builder
public class SkillMetadata {

    // 시전자 정보
    private final PlayerData casterData;
    private final Entity sourceEntity;

    // 현재 대상 정보 (메카닉 실행 시점에 따라 변경될 수 있음)
    private Entity targetEntity;
    private Location targetLocation;

    // 스킬 관련 변수 저장소
    @Builder.Default
    private final Map<String, Object> variables = new HashMap<>();

    // 스킬 고유 ID 또는 실행 인스턴스 ID
    private final UUID instanceId = UUID.randomUUID();

    /**
     * 특정 변수 값을 가져옵니다.
     */
    public <T> T getVariable(String key, Class<T> type) {
        Object val = variables.get(key);
        if (type.isInstance(val)) {
            return type.cast(val);
        }
        return null;
    }

    /**
     * 변수 값을 설정합니다.
     */
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    /**
     * 메타데이터의 얕은 복사본을 생성합니다. (타겟 변경 시 유용)
     */
    public SkillMetadata copy() {
        SkillMetadata copy = SkillMetadata.builder()
                .casterData(this.casterData)
                .sourceEntity(this.sourceEntity)
                .targetEntity(this.targetEntity)
                .targetLocation(this.targetLocation)
                .build();
        copy.getVariables().putAll(this.variables);
        return copy;
    }
}

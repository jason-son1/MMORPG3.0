package com.antigravity.rpg.feature.skill.condition;

import com.antigravity.rpg.api.skill.Condition;
import com.antigravity.rpg.feature.skill.condition.impl.*;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import java.util.Map;

/**
 * YAML 설정으로부터 Condition 인스턴스를 생성하고 설정하는 팩토리 클래스입니다.
 */
@Singleton
public class ConditionFactory {

    private final Injector injector;

    @Inject
    public ConditionFactory(Injector injector) {
        this.injector = injector;
    }

    /**
     * 타입과 설정을 기반으로 새로운 Condition 인스턴스를 생성합니다.
     */
    public Condition create(String type, Map<String, Object> config) {
        if (type == null)
            return null;

        Condition condition;
        switch (type.toUpperCase()) {
            case "STAT":
                condition = new StatCondition();
                break;
            case "COMPONENT":
                condition = new ComponentCondition();
                break;
            case "CLASS":
                condition = new ClassCondition();
                break;
            case "ITEM":
                condition = new ItemCondition();
                break;
            case "MYTHIC_MOB":
            case "MYTHIC_MOB_FACTION":
                condition = new MythicMobCondition();
                break;
            case "PAPI":
                condition = new PAPIPlaceholderCondition();
                break;
            // 레거시 지원
            case "HEALTH":
                condition = injector.getInstance(HealthCondition.class);
                break;
            case "BIOME":
                condition = injector.getInstance(BiomeCondition.class);
                break;
            default:
                return null;
        }

        if (config != null) {
            condition.setup(config);
        }
        return condition;
    }
}

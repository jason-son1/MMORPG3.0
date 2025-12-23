package com.antigravity.rpg.feature.skill.condition;

import com.antigravity.rpg.feature.skill.condition.impl.*;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

/**
 * YAML 설정으로부터 Condition 인스턴스를 생성하는 팩토리 클래스입니다.
 */
@Singleton
public class ConditionFactory {

    private final Injector injector;

    @Inject
    public ConditionFactory(Injector injector) {
        this.injector = injector;
    }

    public Condition create(String type) {
        if (type == null)
            return null;

        switch (type.toUpperCase()) {
            case "HEALTH":
                return injector.getInstance(HealthCondition.class);
            case "BIOME":
                return injector.getInstance(BiomeCondition.class);
            case "HOLDING_ITEM":
                return injector.getInstance(HoldingItemCondition.class);
            case "LUA":
                return injector.getInstance(LuaCondition.class);
            default:
                return null;
        }
    }
}

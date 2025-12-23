package com.antigravity.rpg.feature.skill.effect;

import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 다양한 이펙트 유형을 등록하고 관리하는 레지스트리 클래스입니다.
 */
@Singleton
public class EffectRegistry {

    private final Map<String, Supplier<Effect>> registry = new HashMap<>();

    public void register(String type, Supplier<Effect> supplier) {
        registry.put(type.toLowerCase(), supplier);
    }

    public Effect create(String type) {
        Supplier<Effect> supplier = registry.get(type.toLowerCase());
        return supplier != null ? supplier.get() : null;
    }
}

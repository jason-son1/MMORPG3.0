package com.antigravity.rpg.feature.skill.effect;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Map;

/**
 * YAML 설정으로부터 Effect 인스턴스를 생성하는 팩토리 클래스입니다.
 */
@Singleton
public class EffectFactory {

    private final EffectRegistry registry;
    private final EffectLibrary library;

    @Inject
    public EffectFactory(EffectRegistry registry, EffectLibrary library) {
        this.registry = registry;
        this.library = library;
    }

    /**
     * 입력을 기반으로 Effect 객체를 생성하고 설정합니다.
     * 
     * @param config YAML 설정 맵
     * @return 설정된 Effect 객체
     */
    public Effect create(Map<String, Object> config) {
        if (config == null)
            return null;

        Map<String, Object> effectiveConfig = config;

        // 프리셋 적용
        if (config.containsKey("preset")) {
            String presetId = (String) config.get("preset");
            Map<String, Object> preset = library.getPreset(presetId);
            if (preset != null) {
                // 프리셋 복사 후 현재 설정으로 덮어쓰기
                effectiveConfig = new java.util.HashMap<>(preset);
                effectiveConfig.putAll(config);
            }
        }

        if (!effectiveConfig.containsKey("type")) {
            return null;
        }

        String type = (String) effectiveConfig.get("type");
        Effect effect = registry.create(type);

        if (effect != null) {
            effect.setup(effectiveConfig);
        }

        return effect;
    }
}

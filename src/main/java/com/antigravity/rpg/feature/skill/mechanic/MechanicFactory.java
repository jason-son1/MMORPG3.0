package com.antigravity.rpg.feature.skill.mechanic;

import com.antigravity.rpg.feature.skill.mechanic.impl.*;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

/**
 * 메카닉 타입을 기반으로 구현체를 생성하는 팩토리 클래스입니다.
 */
@Singleton
public class MechanicFactory {

    private final Injector injector;
    private final Map<String, Class<? extends Mechanic>> registry = new HashMap<>();

    @Inject
    public MechanicFactory(Injector injector) {
        this.injector = injector;

        // 기본 메카닉 등록
        registry.put("DAMAGE", DamageMechanic.class);
        registry.put("HEAL", HealMechanic.class);
        registry.put("SOUND", SoundMechanic.class);
        registry.put("PROJECTILE", ProjectileMechanic.class);
        registry.put("HELIX", HelixMechanic.class);
        registry.put("SLASH", SlashMechanic.class);
        registry.put("CHANCE", ChanceMechanic.class);
        registry.put("LOOP", LoopMechanic.class);
        registry.put("EFFECT", EffectMechanic.class);
        registry.put("SCRIPT", ScriptMechanic.class); // Lua 스크립트 실행 메카닉
    }

    public Mechanic create(String type) {
        Class<? extends Mechanic> clazz = registry.get(type.toUpperCase());
        if (clazz == null)
            return null;
        return injector.getInstance(clazz);
    }
}

package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.feature.skill.effect.Effect;
import com.antigravity.rpg.feature.skill.effect.EffectFactory;
import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import com.google.inject.Inject;
import org.bukkit.entity.Entity;

import java.util.Map;

/**
 * 이펙트를 재생하는 메카닉입니다.
 * EffectFactory를 통해 실제 이펙트(파티클, 사운드, 모델 등)를 처리합니다.
 */
public class EffectMechanic implements Mechanic {

    private final EffectFactory effectFactory;

    @Inject
    public EffectMechanic(EffectFactory effectFactory) {
        this.effectFactory = effectFactory;
    }

    @Override
    public void cast(SkillCastContext ctx, Map<String, Object> config) {
        // config 자체가 effect 설정 또는 effect 키 구득
        Map<String, Object> effectConfig = config;
        if (config.containsKey("effect") && config.get("effect") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> inner = (Map<String, Object>) config.get("effect");
            effectConfig = inner;
        }

        Effect effect = effectFactory.create(effectConfig);
        if (effect == null)
            return;

        boolean async = (boolean) config.getOrDefault("async", false);

        Runnable playTask = () -> {
            if (ctx.getTargets().isEmpty()) {
                effect.play(ctx.getOriginLocation(), null, ctx);
            } else {
                for (Entity target : ctx.getTargets()) {
                    effect.play(null, target, ctx);
                }
            }
        };

        // Async 처리는 상위 시스템에서 처리하는 것이 일반적이나,
        // 메카닉 레벨에서 명시적으로 요구되면 처리 가능.
        // 여기서는 직접 실행 (Bukkit API는 Sync 권장)
        playTask.run();
    }
}

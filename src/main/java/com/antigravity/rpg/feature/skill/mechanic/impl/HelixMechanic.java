package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.feature.skill.effect.Effect;
import com.antigravity.rpg.feature.skill.effect.EffectFactory;
import com.antigravity.rpg.api.skill.Mechanic;
import com.google.inject.Inject;
import org.bukkit.Location;

import java.util.Map;

/**
 * 나선형(Helix) 패턴으로 이펙트를 재생하는 메카닉입니다.
 */
public class HelixMechanic implements Mechanic {

    private final EffectFactory effectFactory;

    @Inject
    public HelixMechanic(EffectFactory effectFactory) {
        this.effectFactory = effectFactory;
    }

    @Override
    public void cast(SkillCastContext ctx, Map<String, Object> config) {
        double radius = ((Number) config.getOrDefault("radius", 3.0)).doubleValue();
        double height = ((Number) config.getOrDefault("height", 2.0)).doubleValue();
        double step = ((Number) config.getOrDefault("step", 0.5)).doubleValue();

        @SuppressWarnings("unchecked")
        Map<String, Object> effectConfig = (Map<String, Object>) config.get("effect");
        Effect effect = effectFactory.create(effectConfig);

        if (effect == null)
            return;

        Location center = ctx.getOriginLocation();

        // 나선형 계산 및 이펙트 재생
        for (double y = 0; y <= height; y += step) {
            double angle = y * 2.0 * Math.PI; // 간단한 회전
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            Location point = center.clone().add(x, y, z);
            effect.play(point, null, ctx);
        }
    }
}

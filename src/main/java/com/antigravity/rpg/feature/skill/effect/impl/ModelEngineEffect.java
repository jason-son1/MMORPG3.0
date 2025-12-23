package com.antigravity.rpg.feature.skill.effect.impl;

import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.feature.skill.effect.Effect;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Map;

/**
 * ModelEngine을 이용한 모델 애니메이션 효과 구현체입니다.
 */
public class ModelEngineEffect implements Effect {

    private String animation;
    private float lerpIn = 0.1f;
    private float lerpOut = 0.1f;
    private float speed = 1.0f;
    private boolean force = true;

    @Override
    public void setup(Map<String, Object> config) {
        this.animation = (String) config.get("animation");
        this.lerpIn = ((Number) config.getOrDefault("lerp-in", 0.1f)).floatValue();
        this.lerpOut = ((Number) config.getOrDefault("lerp-out", 0.1f)).floatValue();
        this.speed = ((Number) config.getOrDefault("speed", 1.0f)).floatValue();
        this.force = (boolean) config.getOrDefault("force", true);
    }

    @Override
    public void play(Location origin, Entity target, SkillCastContext ctx) {
        if (target == null || animation == null)
            return;

        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(target.getUniqueId());
        if (modeledEntity != null) {
            for (ActiveModel model : modeledEntity.getModels().values()) {
                model.getAnimationHandler().playAnimation(animation, lerpIn, lerpOut, speed, force);
            }
        }
    }
}

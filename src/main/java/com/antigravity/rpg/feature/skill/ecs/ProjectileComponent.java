package com.antigravity.rpg.feature.skill.ecs;

import com.antigravity.rpg.core.ecs.Component;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;

/**
 * ECS 기반 투사체의 상태를 저장하는 컴포넌트입니다.
 */
@Getter
@Setter
public class ProjectileComponent implements Component {

    private Location currentLocation;
    private final Vector velocity;

    private final List<Map<String, Object>> onTickMechanics;
    private final List<Map<String, Object>> onHitMechanics;

    private int lifeTicksRemaining = 200; // 최대 10초
    private final SkillCastContext context;

    private final double hitboxSize;
    private final boolean ignoreCaster;

    public ProjectileComponent(Location start, Vector velocity,
            List<Map<String, Object>> onTick,
            List<Map<String, Object>> onHit,
            SkillCastContext context,
            double hitboxSize,
            boolean ignoreCaster) {
        this.currentLocation = start;
        this.velocity = velocity;
        this.onTickMechanics = onTick;
        this.onHitMechanics = onHit;
        this.context = context;
        this.hitboxSize = hitboxSize;
        this.ignoreCaster = ignoreCaster;
    }
}

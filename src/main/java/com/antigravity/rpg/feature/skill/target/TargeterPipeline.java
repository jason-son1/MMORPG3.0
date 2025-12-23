package com.antigravity.rpg.feature.skill.target;

import com.antigravity.rpg.feature.skill.condition.Condition;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import lombok.Builder;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AOE → Filter(조건) → Sort → Limit 파이프라인을 처리하는 타겟터 구현체입니다.
 */
@Builder
public class TargeterPipeline implements Targeter {

    private final Targeter baseTargeter; // AOE (Sphere, Cone 등)
    private final List<Condition> filters; // Filter
    private final String sortBy; // Sort (HEALTH, DISTANCE, THREAT)
    private final int limit; // Limit

    @Override
    public List<Entity> getTargetEntities(SkillCastContext ctx) {
        List<Entity> targets = baseTargeter.getTargetEntities(ctx);

        // 1. Filter
        if (filters != null && !filters.isEmpty()) {
            targets = targets.stream()
                    .filter(entity -> filters.stream().allMatch(cond -> cond.evaluate(ctx, entity)))
                    .collect(Collectors.toList());
        }

        // 2. Sort
        if (sortBy != null) {
            switch (sortBy.toUpperCase()) {
                case "HEALTH":
                    targets.sort(Comparator.comparingDouble(e -> (e instanceof org.bukkit.entity.LivingEntity)
                            ? ((org.bukkit.entity.LivingEntity) e).getHealth()
                            : 0));
                    break;
                case "DISTANCE":
                    Location origin = ctx.getOriginLocation();
                    targets.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(origin)));
                    break;
                // THREAT 등은 추후 확장
            }
        }

        // 3. Limit
        if (limit > 0 && targets.size() > limit) {
            targets = targets.subList(0, limit);
        }

        return targets;
    }

    @Override
    public List<Location> getTargetLocations(SkillCastContext ctx) {
        // 위치 기반 타겟팅은 보통 필터나 정렬이 제한적이므로 기본 타겟터 결과 반환
        return baseTargeter.getTargetLocations(ctx);
    }
}

package com.antigravity.rpg.feature.classes.ai;

import com.antigravity.rpg.feature.classes.ClassDefinition;
import com.google.inject.Singleton;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 직업에 정의된 AI 행동 패턴을 처리하는 클래스입니다.
 */
@Singleton
public class AIProcessor {

    /**
     * AI 우선순위에 따라 주변 엔티티 중 최적의 타겟을 선택합니다.
     *
     * @param source   AI 주체
     * @param priority 우선순위 타입
     * @param range    탐색 범위
     * @return 선택된 타겟
     */
    public Optional<LivingEntity> selectTarget(LivingEntity source, ClassDefinition.TargetPriority priority,
            double range) {
        List<Entity> nearby = source.getNearbyEntities(range, range, range);

        return nearby.stream()
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity) e)
                .filter(e -> !e.equals(source))
                .filter(e -> !(e instanceof Player p && p.getGameMode().name().contains("SPECTATOR")))
                .min(getComparator(source, priority));
    }

    private Comparator<LivingEntity> getComparator(LivingEntity source, ClassDefinition.TargetPriority priority) {
        return switch (priority) {
            case CLOSEST -> Comparator.comparingDouble(e -> e.getLocation().distanceSquared(source.getLocation()));
            case LOWEST_HP -> Comparator.comparingDouble(LivingEntity::getHealth);
            case HIGHEST_THREAT -> (e1, e2) -> {
                // 위협도 로직이 구현되어 있다면 여기서 비교 (현재는 임의로 처리)
                return Double.compare(e2.getHealth(), e1.getHealth());
            };
            case RANDOM -> (e1, e2) -> Math.random() > 0.5 ? 1 : -1;
            default -> Comparator.comparingDouble(e -> e.getLocation().distanceSquared(source.getLocation()));
        };
    }

    /**
     * 스킬 로테이션 조건이 충족되는지 확인합니다.
     */
    public boolean checkSkillCondition(String condition, LivingEntity source, LivingEntity target) {
        if (condition == null || condition.isEmpty() || condition.equalsIgnoreCase("always")) {
            return true;
        }

        // 간단한 조건 처리 (추후 ExpressionEngine 연동 가능)
        if (condition.startsWith("target_distance > ")) {
            double dist = Double.parseDouble(condition.replace("target_distance > ", ""));
            return target != null && source.getLocation().distance(target.getLocation()) > dist;
        }

        return false;
    }
}

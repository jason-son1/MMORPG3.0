package com.antigravity.rpg.core.engine;

import com.antigravity.rpg.core.formula.ExpressionEngine;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 엔티티의 최종 스탯을 계산하는 클래스입니다.
 * 수식 계산 및 마인크래프트 기본 스탯과의 연동을 담당하며, 성능을 위해 틱 단위 캐싱을 수행합니다.
 */
@Singleton
public class StatCalculator {

    private final StatRegistry statRegistry;
    private final ExpressionEngine expressionEngine;
    private final com.antigravity.rpg.feature.classes.ClassRegistry classRegistry;

    // Persistent Cache: Map<HolderUUID, Map<StatId, Value>>
    // Not cleared every tick, but invalidated on dirty events.
    private final Map<UUID, Map<String, Double>> statCache = new ConcurrentHashMap<>();

    // [NEW] 틱 단위 직업 스탯 캐시 (Lua 결과 캐싱)
    private final Map<UUID, ClassStatCacheEntry> classStatCache = new ConcurrentHashMap<>();

    // 순환 참조 감지용 스택 (스레드별로 현재 계산 중인 스탯 ID 추적)
    private final ThreadLocal<java.util.Set<String>> calculationStack = ThreadLocal.withInitial(java.util.HashSet::new);

    @Inject
    public StatCalculator(StatRegistry statRegistry, ExpressionEngine expressionEngine,
            com.antigravity.rpg.feature.classes.ClassRegistry classRegistry) {
        this.statRegistry = statRegistry;
        this.expressionEngine = expressionEngine;
        this.classRegistry = classRegistry;
    }

    /**
     * Mark stats as dirty for a holder, clearing the cache.
     * Should be called on equipment change, buff application, etc.
     */
    public void invalidate(UUID holderId) {
        statCache.remove(holderId);
        classStatCache.remove(holderId);
    }

    // For specific stat invalidation if needed:
    // public void invalidate(UUID holderId, String statId) { ... }

    /**
     * 대상의 최종 스탯 값을 계산합니다. (캐시 우선 확인)
     * 
     * @param holder 스탯 보유 대상
     * @param statId 스탯 식별자
     * @return 계산된 최종 값
     */
    public double getStat(StatHolder holder, String statId) {
        UUID holderId = getHolderId(holder);

        // 1. 캐시 확인
        if (holderId != null) {
            Map<String, Double> holderCache = statCache.get(holderId);
            if (holderCache != null && holderCache.containsKey(statId)) {
                return holderCache.get(statId);
            }
        }

        // 2. 실제 계산 수행
        double result = calculateStat(holder, statId);

        // 3. 캐시에 저장
        if (holderId != null) {
            statCache.computeIfAbsent(holderId, k -> new ConcurrentHashMap<>()).put(statId, result);
        }

        return result;
    }

    private double calculateStat(StatHolder holder, String statId) {
        // 순환 참조 감지: 이미 이 스탯을 계산 중이면 기본값 반환
        java.util.Set<String> stack = calculationStack.get();
        if (!stack.add(statId)) {
            // 순환 참조 감지됨 - 무한 루프 방지를 위해 기본값 반환
            return 0.0;
        }

        try {
            StatDefinition def = statRegistry.getStat(statId).orElse(null);

            // 1. 기본값 및 원본 데이터 가져오기 (장비, 영구 보너스 등)
            double baseValue = getRawValue(holder, statId, def);

            // 2. 직업 기반 성장/스탯 계산 (Lua 우선, Multi-Class 100% + 30%)
            if (holder instanceof com.antigravity.rpg.feature.player.PlayerData pd) {
                // 캐시 확인 및 생성
                ClassStatCacheEntry cacheEntry = classStatCache.get(pd.getUuid());
                if (cacheEntry == null) {
                    Map<String, Double> totalStats = new java.util.HashMap<>();
                    java.util.Set<String> handled = new java.util.HashSet<>();

                    var activeClasses = pd.getClassData().getActiveClasses();
                    if (activeClasses != null) {
                        for (Map.Entry<com.antigravity.rpg.feature.player.ClassType, String> entry : activeClasses
                                .entrySet()) {
                            com.antigravity.rpg.feature.player.ClassType type = entry.getKey();
                            String cId = entry.getValue();
                            if (cId == null || cId.isEmpty())
                                continue;

                            double multiplier = (type == com.antigravity.rpg.feature.player.ClassType.MAIN) ? 1.0
                                    : (type == com.antigravity.rpg.feature.player.ClassType.SUB ? 0.3 : 0.0);

                            if (multiplier <= 0)
                                continue;

                            // 레벨 가져오기
                            int level = 1;
                            var cp = pd.getClassData().getProgress(cId);
                            if (cp != null)
                                level = cp.getLevel();

                            var cDefOpt = classRegistry.getClass(cId);
                            if (cDefOpt.isPresent()) {
                                var cDef = cDefOpt.get();
                                Map<String, Double> luaStats = cDef.calculateStats(pd, level);
                                if (luaStats != null) {
                                    // Lua 스탯 합산
                                    for (Map.Entry<String, Double> stat : luaStats.entrySet()) {
                                        totalStats.merge(stat.getKey(), stat.getValue() * multiplier, Double::sum);
                                    }
                                    handled.add(cId);
                                }
                            }
                        }
                    }
                    cacheEntry = new ClassStatCacheEntry(totalStats, handled);
                    classStatCache.put(pd.getUuid(), cacheEntry);
                }

                // A. Lua 계산된 스탯 적용
                baseValue += cacheEntry.totalStats.getOrDefault(statId, 0.0);

                // B. Lua 로직이 없는 클래스에 대해 YAML 성장 로직 수행
                var activeClasses = pd.getClassData().getActiveClasses();
                if (activeClasses != null) {
                    for (Map.Entry<com.antigravity.rpg.feature.player.ClassType, String> entry : activeClasses
                            .entrySet()) {
                        String cId = entry.getValue();
                        if (cId == null || cId.isEmpty())
                            continue;

                        // 이미 Lua로 처리된 클래스는 스킵
                        if (cacheEntry.handled.contains(cId))
                            continue;

                        double multiplier = (entry.getKey() == com.antigravity.rpg.feature.player.ClassType.MAIN) ? 1.0
                                : (entry.getKey() == com.antigravity.rpg.feature.player.ClassType.SUB ? 0.3 : 0.0);
                        if (multiplier <= 0)
                            continue;

                        int level = 1;
                        var cp = pd.getClassData().getProgress(cId);
                        if (cp != null)
                            level = cp.getLevel();

                        var cDefOpt = classRegistry.getClass(cId);
                        if (cDefOpt.isPresent()) {
                            var cDef = cDefOpt.get();
                            if (cDef.getGrowth() != null && cDef.getGrowth().getPerLevel() != null) {
                                String growthExpr = cDef.getGrowth().getPerLevel().get(statId);
                                if (growthExpr != null) {
                                    double growthVal = 0.0;
                                    if (isNumeric(growthExpr)) {
                                        growthVal = Double.parseDouble(growthExpr) * (level - 1);
                                    } else { // 수식 평가
                                        growthVal = expressionEngine.evaluate(growthExpr, holder);
                                    }
                                    baseValue += growthVal * multiplier;
                                }
                            }
                        }
                    }
                }
            }

            // 3. 파생 스탯 보너스(Bonuses) 적용
            for (StatRegistry.StatBonus bonus : statRegistry.getBonuses()) {
                if (bonus.getTarget().equals(statId)) {
                    if (calculationStack.get().contains(bonus.getSource())) {
                        continue;
                    }

                    double sourceValue = getStat(holder, bonus.getSource());
                    String safeFormula = bonus.getFormula().replace("source", String.valueOf(sourceValue));
                    baseValue += expressionEngine.evaluateFormula(safeFormula, holder);
                }
            }

            if (def == null)
                return baseValue;

            switch (def.getType()) {
                case FORMULA:
                    return expressionEngine.evaluate(def.getFormula(), holder);

                case NATIVE_ATTRIBUTE:
                    // 마인크래프트 기본 속성(Attribute) 연동
                    if (holder instanceof NativeStatHolder) {
                        return ((NativeStatHolder) holder).getNativeAttributeValue(def.getNativeAttribute());
                    }
                    return baseValue;

                case RESOURCE:
                case SIMPLE:
                case CHANCE:
                default:
                    return baseValue;
            }
        } finally {
            // 계산 완료 후 스택에서 제거
            calculationStack.get().remove(statId);
        }
    }

    private boolean isNumeric(String str) {
        if (str == null)
            return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private double getRawValue(StatHolder holder, String statId, StatDefinition def) {
        if (holder instanceof PlayerDataFunc) {
            return ((PlayerDataFunc) holder).getRawStat(statId);
        }
        return def != null ? def.getDefaultValue() : 0.0;
    }

    /**
     * 매 틱마다 호출하여 캐시를 초기화하던 메서드입니다.
     * 이제 Dirty Flag 시스템을 사용하므로, 전체 초기화가 필요한 경우에만 사용하세요.
     */
    public void clearAllCache() {
        statCache.clear();
        classStatCache.clear();
    }

    private UUID getHolderId(StatHolder holder) {
        if (holder instanceof com.antigravity.rpg.feature.player.PlayerData) {
            return ((com.antigravity.rpg.feature.player.PlayerData) holder).getUuid();
        }
        return null;
    }

    private static class ClassStatCacheEntry {
        final Map<String, Double> totalStats;
        final java.util.Set<String> handled;

        ClassStatCacheEntry(Map<String, Double> totalStats, java.util.Set<String> handled) {
            this.totalStats = totalStats;
            this.handled = handled;
        }
    }

    /**
     * 스탯 보유 대상이 원본 데이터를 제공하기 위한 인터페이스입니다.
     */
    public interface PlayerDataFunc extends StatHolder {
        double getRawStat(String statId);
    }

    /**
     * 스탯 보유 대상이 마인크래프트 기본 속성을 제공하기 위한 인터페이스입니다.
     */
    public interface NativeStatHolder extends StatHolder {
        double getNativeAttributeValue(String attributeName);
    }
}

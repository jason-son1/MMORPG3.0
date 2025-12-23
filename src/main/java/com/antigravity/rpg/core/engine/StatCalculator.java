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

    // 틱 단위 캐시: Map<HolderUUID, Map<StatId, Value>>
    private final Map<UUID, Map<String, Double>> tickCache = new ConcurrentHashMap<>();

    @Inject
    public StatCalculator(StatRegistry statRegistry, ExpressionEngine expressionEngine) {
        this.statRegistry = statRegistry;
        this.expressionEngine = expressionEngine;
    }

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
            Map<String, Double> holderCache = tickCache.get(holderId);
            if (holderCache != null && holderCache.containsKey(statId)) {
                return holderCache.get(statId);
            }
        }

        // 2. 실제 계산 수행
        double result = calculateStat(holder, statId);

        // 3. 캐시에 저장
        if (holderId != null) {
            tickCache.computeIfAbsent(holderId, k -> new ConcurrentHashMap<>()).put(statId, result);
        }

        return result;
    }

    private double calculateStat(StatHolder holder, String statId) {
        StatDefinition def = statRegistry.getStat(statId).orElse(null);
        if (def == null) {
            // 정의되지 않은 스탯은 원본 데이터에서 조회
            if (holder instanceof PlayerDataFunc) {
                return ((PlayerDataFunc) holder).getRawStat(statId);
            }
            return 0.0;
        }

        switch (def.getType()) {
            case FORMULA:
                return expressionEngine.evaluate(def.getFormula(), holder);

            case NATIVE_ATTRIBUTE:
                // 마인크래프트 기본 속성(Attribute) 연동
                if (holder instanceof NativeStatHolder) {
                    return ((NativeStatHolder) holder).getNativeAttributeValue(def.getNativeAttribute());
                }
                return getRawValue(holder, statId, def);

            case RESOURCE:
            case SIMPLE:
            case CHANCE:
            default:
                return getRawValue(holder, statId, def);
        }
    }

    private double getRawValue(StatHolder holder, String statId, StatDefinition def) {
        if (holder instanceof PlayerDataFunc) {
            return ((PlayerDataFunc) holder).getRawStat(statId);
        }
        return def.getDefaultValue();
    }

    /**
     * 매 틱마다 호출하여 캐시를 초기화해야 합니다.
     */
    public void clearCache() {
        tickCache.clear();
    }

    private UUID getHolderId(StatHolder holder) {
        if (holder instanceof com.antigravity.rpg.feature.player.PlayerData) {
            return ((com.antigravity.rpg.feature.player.PlayerData) holder).getUuid();
        }
        // 다른 타입의 Holder가 추가될 경우 여기에 로직 확장 가능
        return null;
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

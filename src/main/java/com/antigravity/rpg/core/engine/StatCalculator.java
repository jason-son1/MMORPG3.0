package com.antigravity.rpg.core.engine;

import com.antigravity.rpg.core.formula.ExpressionEngine;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class StatCalculator {

    private final StatRegistry statRegistry;
    private final ExpressionEngine expressionEngine;

    @Inject
    public StatCalculator(StatRegistry statRegistry, ExpressionEngine expressionEngine) {
        this.statRegistry = statRegistry;
        this.expressionEngine = expressionEngine;
    }

    public double getStat(StatHolder holder, String statId) {
        StatDefinition def = statRegistry.getStat(statId).orElse(null);
        if (def == null) {
            // 정의되지 않은 스탯은 단순 데이터 저장소 값으로 처리하거나 0 반환
            // 여기서는 StatHolder의 raw 값을 시도해보고 없으면 0이라고 가정할 수는 없으므로(인터페이스 제약),
            // holder가 스스로 처리할 수 있도록 해야 하지만,
            // 순환 호출 구조상 holder.getStat이 이 메서드를 호출하는 구조라면 무한 루프 위험.
            // 따라서 holder가 getRawStat을 제공해야 함.
            if (holder instanceof PlayerDataFunc) {
                return ((PlayerDataFunc) holder).getRawStat(statId);
            }
            return 0.0;
        }

        switch (def.getType()) {
            case FORMULA:
                return expressionEngine.evaluate(def.getFormula(), holder);

            case NATIVE_ATTRIBUTE:
                // Minecraft Attribute와 동기화된 값을 가져와야 함.
                // 보통 NATIVE는 기본값 + 장비/버프 보정치.
                // 여기서는 StatHolder가 직접 Attribute 값을 조회할 수 있다고 가정.
                // 만약 holder가 Entity 기반이라면 실제 Attribute 값을, 아니면 raw 값을 반환.
                if (holder instanceof NativeStatHolder) {
                    return ((NativeStatHolder) holder).getNativeAttributeValue(def.getNativeAttribute());
                }
                return getRawValue(holder, statId, def);

            case RESOURCE:
                // 리소스는 보통 현재 값을 의미. 최대값은 별도 스탯(예: max_health)으로 정의됨.
                // 단순히 저장된 값을 반환.
                return getRawValue(holder, statId, def);

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

    // 내부 사용 인터페이스 (임시) - 나중에 메인 파일로 이동 가능
    public interface PlayerDataFunc extends StatHolder {
        double getRawStat(String statId);
    }

    public interface NativeStatHolder extends StatHolder {
        double getNativeAttributeValue(String attributeName);
    }
}

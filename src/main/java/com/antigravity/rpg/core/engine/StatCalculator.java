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

    // 순환 참조 감지용 스택 (스레드별로 현재 계산 중인 스탯 ID 추적)
    private final ThreadLocal<java.util.Set<String>> calculationStack = ThreadLocal.withInitial(java.util.HashSet::new);

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

            // 2. 직업 기반 성장 보너스 계산
            if (holder instanceof com.antigravity.rpg.feature.player.PlayerData pd) {
                String classId = pd.getClassId();
                if (classId != null && !classId.isEmpty()) {
                    var classDefOpt = com.antigravity.rpg.feature.player.PlayerData.getClassRegistry()
                            .getClass(classId);
                    if (classDefOpt.isPresent()) {
                        var cDef = classDefOpt.get();

                        // 성장 스탯 합산: total = base(attributes.base) + eval(formula, level)
                        if (cDef.getGrowth() != null && cDef.getGrowth().getPerLevel() != null) {
                            String growthExpr = cDef.getGrowth().getPerLevel().get(statId);
                            if (growthExpr != null) {
                                if (isNumeric(growthExpr)) {
                                    double growthValue = Double.parseDouble(growthExpr);
                                    int level = pd.getLevel();
                                    baseValue += (level - 1) * growthValue;
                                } else {
                                    // 수식 평가 (예: "2 + (level * 0.5)")
                                    double evaluatedGrowth = expressionEngine.evaluate(growthExpr, holder);
                                    baseValue += evaluatedGrowth;
                                }
                            }
                        }
                    }
                }
            }

            // 3. 파생 스탯 보너스(Bonuses) 적용
            // "source" -> "target" 공식이 있다면, holder.getStat(source)를 조회하여 공식 적용
            // 주의: 순환 참조 방지를 위해 calculateStat 내부에서는 getStat(source)를 신중하게 호출해야 하지만,
            // 여기서는 getStat(source)가 캐시를 타므로 무한 루프만 없다면 괜찮음.
            // 하지만 getStat(source)가 다시 이 calculateStat(target)을 부르는 구조면 스택 오버플로우.
            // 보통 Primary(힘) -> Secondary(공격력) 구조이므로 상호 참조는 설정 단계에서 막아야 함.

            for (StatRegistry.StatBonus bonus : statRegistry.getBonuses()) {
                if (bonus.getTarget().equals(statId)) {
                    // source 값을 가져옴 -> getStat 재귀 호출
                    // 순환 참조 감지: 이미 계산 중인 스탯이면 건너뜀
                    if (calculationStack.get().contains(bonus.getSource())) {
                        // 순환 참조 발생 - 기본값 0으로 처리하고 경고 로깅
                        continue;
                    }

                    double sourceValue = getStat(holder, bonus.getSource());

                    // 공식에서 "source" 변수에 sourceValue를 주입하기 위해 임시 변수 치환
                    // 하지만 ExpressionEngine.evaluate(formula, holder)는 {var} 형태를 holder에서 찾음.
                    // 여기서는 "source"라는 특정 변수명을 값으로 치환해서 전달해야 함.
                    // 편의상 수식 문자열 자체를 replace하거나,
                    // ExpressionEngine에 setVariable 기능을 외부에서 쓸 수 있게 해야 하는데,
                    // 현재 evaluate 인터페이스는 holder만 받음.
                    // 간단한 해결책: 수식 내 "source"를 숫자값으로 치환해버리기.
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

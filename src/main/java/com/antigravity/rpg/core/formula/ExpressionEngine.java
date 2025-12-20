package com.antigravity.rpg.core.formula;

import com.antigravity.rpg.core.engine.StatHolder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

/**
 * 수식 계산을 담당하는 엔진입니다.
 * exp4j 라이브러리를 사용합니다.
 */
@Singleton
public class ExpressionEngine {

    private final PlaceholderService placeholderService;

    @Inject
    public ExpressionEngine(PlaceholderService placeholderService) {
        this.placeholderService = placeholderService;
    }

    /**
     * 수식을 계산하여 결과를 반환합니다.
     * 
     * @param formula 수식 문자열 (예: "20 + {vitality} * 2")
     * @param holder  스탯 정보를 제공할 대상
     * @return 계산된 실수 값
     */
    public double evaluate(String formula, StatHolder holder) {
        if (formula == null || formula.isEmpty())
            return 0.0;

        try {
            // 1. 플레이스홀더 치환
            String parsedFormula = placeholderService.parse(formula, holder);

            // 2. 수식 계산
            Expression expression = new ExpressionBuilder(parsedFormula).build();
            return expression.evaluate();
        } catch (Exception e) {
            // 수식 오류 시 0 반환 및 로그 (실제로는 로거 사용 권장)
            e.printStackTrace();
            return 0.0;
        }
    }
}

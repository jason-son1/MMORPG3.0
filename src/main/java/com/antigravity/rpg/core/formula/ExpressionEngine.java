package com.antigravity.rpg.core.formula;

import com.antigravity.rpg.AntiGravityPlugin;
import com.antigravity.rpg.core.engine.StatHolder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 수식 계산을 담당하는 엔진입니다.
 * exp4j 라이브러리를 사용하며, 성능 향상을 위해 컴파일된 수식을 캐싱합니다.
 */
@Singleton
public class ExpressionEngine {

    // 컴파일된 수식을 저장하는 캐시 (성능 최적화)
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");

    @Inject
    public ExpressionEngine(AntiGravityPlugin plugin) {
        this.plugin = plugin;
    }

    private final AntiGravityPlugin plugin;

    /**
     * config.yml의 formulas 섹션에 정의된 수식을 가져와 계산합니다.
     * 
     * @param formulaKey config.yml 내의 키 (예: "formulas.damage-reduction")
     * @param holder     스탯 정보를 제공할 대상
     * @return 계산된 실수 값. 수식이 없거나 오류 생기면 0.0
     */
    public double evaluate(String formulaKey, StatHolder holder) {
        String formula = plugin.getConfig().getString("formulas." + formulaKey);
        if (formula == null) {
            // 키 전체를 인자로 받았을 수도 있으므로 재시도 (legacy support or direct path)
            formula = plugin.getConfig().getString(formulaKey);
        }

        if (formula == null || formula.isEmpty()) {
            // plugin.getLogger().warning("Formula not found in config: " + formulaKey);
            return 0.0;
        }
        return evaluateFormula(formula, holder);
    }

    /**
     * 수식을 계산하여 결과를 반환합니다.
     * 
     * @param formula 수식 문자열 (예: "20 + {vitality} * 2")
     * @param holder  스탯 정보를 제공할 대상
     * @return 계산된 실수 값
     */
    public double evaluateFormula(String formula, StatHolder holder) {
        if (formula == null || formula.isEmpty())
            return 0.0;

        try {
            // 1. 캐시에서 컴파일된 수식 가져오기 또는 생성
            Expression expression = expressionCache.computeIfAbsent(formula, f -> {
                // 수식에서 변수({key}) 추출
                Matcher matcher = PLACEHOLDER_PATTERN.matcher(f);
                ExpressionBuilder builder = new ExpressionBuilder(f.replaceAll("\\{", "").replaceAll("\\}", ""));

                java.util.Set<String> variables = new java.util.HashSet<>();
                while (matcher.find()) {
                    variables.add(matcher.group(1));
                }

                if (!variables.isEmpty()) {
                    builder.variables(variables);
                }
                return builder.build();
            });

            // 2. 변수 값 설정
            for (String var : expression.getVariableNames()) {
                double val = (holder != null) ? holder.getStat(var) : 0.0;
                expression.setVariable(var, val);
            }

            // 3. 계산 결과 반환
            return expression.evaluate();
        } catch (Exception e) {
            // 수식 오류 시 0 반환 및 에러 출력
            e.printStackTrace();
            return 0.0;
        }
    }

    /**
     * 캐시된 수식을 모두 삭제합니다. (설정 리로드 시 호출 권장)
     */
    public void clearCache() {
        expressionCache.clear();
    }
}

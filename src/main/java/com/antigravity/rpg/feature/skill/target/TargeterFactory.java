package com.antigravity.rpg.feature.skill.target;

import com.antigravity.rpg.api.skill.Condition;
import com.antigravity.rpg.feature.skill.condition.ConditionFactory;
import com.antigravity.rpg.api.skill.Targeter;
import com.antigravity.rpg.feature.skill.target.impl.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * YAML 설정으로부터 Targeter 인스턴스를 생성하는 팩토리 클래스입니다.
 * 필터, 정렬, 제한이 포함된 파이프라인 구조를 지원합니다.
 */
@Singleton
public class TargeterFactory {

    private final ConditionFactory conditionFactory;

    @Inject
    public TargeterFactory(ConditionFactory conditionFactory) {
        this.conditionFactory = conditionFactory;
    }

    @SuppressWarnings("unchecked")
    public Targeter create(Map<String, Object> config) {
        if (config == null)
            return new SelfTargeter();

        String type = (String) config.getOrDefault("type", "self");
        Targeter base;

        switch (type.toLowerCase()) {
            case "sphere":
                base = new SphereTargeter(config);
                break;
            case "cone":
                base = new ConeTargeter();
                base.setup(config);
                break;
            case "ray":
            case "raytrace":
                base = new RayTraceTargeter(config);
                break;
            case "ring":
                base = new RingTargeter(config);
                break;
            case "radius":
                base = new RadiusTargeter(config);
                break;
            case "self":
            default:
                base = new SelfTargeter();
                break;
        }

        // 파이프라인 구성 (Filter, Sort, Limit)
        if (config.containsKey("filter") || config.containsKey("sort") || config.containsKey("limit")) {
            TargeterPipeline.TargeterPipelineBuilder builder = TargeterPipeline.builder()
                    .baseTargeter(base);

            // Filter (Universal Conditions 활용)
            if (config.containsKey("filter")) {
                List<Map<String, Object>> filterConfigs = (List<Map<String, Object>>) config.get("filter");
                List<Condition> filters = new ArrayList<>();
                for (Map<String, Object> fc : filterConfigs) {
                    String fType = (String) fc.get("type");
                    Condition cond = conditionFactory.create(fType, fc);
                    if (cond != null)
                        filters.add(cond);
                }
                builder.filters(filters);
            }

            // Sort
            if (config.containsKey("sort")) {
                builder.sortBy((String) config.get("sort"));
            }

            // Limit
            if (config.containsKey("limit")) {
                builder.limit(((Number) config.get("limit")).intValue());
            }

            return builder.build();
        }

        return base;
    }
}

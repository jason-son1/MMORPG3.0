package com.antigravity.rpg.feature.skill.target;

import com.antigravity.rpg.feature.skill.target.impl.*;
import com.google.inject.Singleton;

import java.util.Map;

/**
 * YAML 설정으로부터 Targeter 인스턴스를 생성하는 팩토리 클래스입니다.
 */
@Singleton
public class TargeterFactory {

    public Targeter create(Map<String, Object> config) {
        if (config == null)
            return new SelfTargeter();

        String type = (String) config.getOrDefault("type", "SELF");

        switch (type.toUpperCase()) {
            case "RADIUS":
                double radius = ((Number) config.getOrDefault("radius", 5.0)).doubleValue();
                boolean includeCaster = (boolean) config.getOrDefault("include-caster", false);
                return new RadiusTargeter(radius, includeCaster);
            case "CONE":
                double coneRadius = ((Number) config.getOrDefault("radius", 5.0)).doubleValue();
                double angle = ((Number) config.getOrDefault("angle", 45.0)).doubleValue();
                return new ConeTargeter(coneRadius, angle);
            case "RAYTRACE":
                double distance = ((Number) config.getOrDefault("distance", 10.0)).doubleValue();
                return new RayTraceTargeter(distance);
            case "RING":
                double inner = ((Number) config.getOrDefault("inner-radius", 3.0)).doubleValue();
                double outer = ((Number) config.getOrDefault("outer-radius", 7.0)).doubleValue();
                return new RingTargeter(inner, outer);
            default:
                return new SelfTargeter();
        }
    }
}

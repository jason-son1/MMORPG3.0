package com.antigravity.rpg.feature.skill;

import com.antigravity.rpg.core.registry.Registry;
import com.antigravity.rpg.api.skill.Mechanic;
import com.antigravity.rpg.api.skill.Targeter;
import com.antigravity.rpg.api.skill.Condition;
import com.google.inject.Singleton;
import com.google.inject.Inject;
import com.google.inject.Injector;

@Singleton
public class SkillRegistry {

    private final Injector injector;
    private final Registry<Mechanic> mechanicRegistry = new Registry<>();
    private final Registry<Targeter> targeterRegistry = new Registry<>();
    private final Registry<Condition> conditionRegistry = new Registry<>();

    @Inject
    public SkillRegistry(Injector injector) {
        this.injector = injector;
    }

    public Registry<Mechanic> getMechanics() {
        return mechanicRegistry;
    }

    public Registry<Targeter> getTargeters() {
        return targeterRegistry;
    }

    public Registry<Condition> getConditions() {
        return conditionRegistry;
    }

    public void registerMechanic(String id, Class<? extends Mechanic> clazz) {
        mechanicRegistry.register(id, () -> injector.getInstance(clazz));
    }

    public void registerTargeter(String id, Class<? extends Targeter> clazz) {
        targeterRegistry.register(id, () -> injector.getInstance(clazz));
    }

    public void registerCondition(String id, Class<? extends Condition> clazz) {
        conditionRegistry.register(id, () -> injector.getInstance(clazz));
    }
}
